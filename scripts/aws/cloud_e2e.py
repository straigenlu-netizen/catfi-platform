"""Real AWS end-to-end: JWT + object authorization -> API -> outbox -> SQS -> SF -> S3.
Uses only synthetic data and removes it in finally. Does not test the existing Kafka chain.
"""
import argparse
import json
import secrets
import time
import uuid
import urllib.request
import urllib.error
import boto3


def request(base, method, path, token=None, key=None):
    headers = {}
    if token: headers['Authorization'] = 'Bearer ' + token
    if key: headers['Idempotency-Key'] = key
    req = urllib.request.Request(base + path, method=method, headers=headers,
                                 data=b'{}' if method == 'POST' else None)
    try:
        with urllib.request.urlopen(req, timeout=15) as res:
            return res.status, json.loads(res.read())
    except urllib.error.HTTPError as err:
        return err.code, json.loads(err.read())


def main(stack):
    session = boto3.Session()
    cf = session.client('cloudformation')
    outputs = {x['OutputKey']: x['OutputValue'] for x in cf.describe_stacks(StackName=stack)['Stacks'][0]['Outputs']}
    cognito = session.client('cognito-idp'); db = session.resource('dynamodb'); s3 = session.client('s3')
    table = db.Table(outputs['ReportingTable']); jobs = db.Table(outputs['JobsTable'])
    property_id = str(uuid.uuid4()); email = 'catfi-smoke-' + uuid.uuid4().hex + '@example.com'
    password = 'Aa1!' + secrets.token_urlsafe(24)
    report_key = {'pk': 'PROPERTY#' + property_id, 'sk': 'STATE'}
    access_key = None; job_id = None; object_key = None
    try:
        user = cognito.admin_create_user(UserPoolId=outputs['UserPoolId'], Username=email,
            MessageAction='SUPPRESS', UserAttributes=[{'Name': 'email', 'Value': email}, {'Name': 'email_verified', 'Value': 'true'}])
        sub = next(x['Value'] for x in user['User']['Attributes'] if x['Name'] == 'sub')
        cognito.admin_set_user_password(UserPoolId=outputs['UserPoolId'], Username=email, Password=password, Permanent=True)
        token = cognito.initiate_auth(ClientId=outputs['UserPoolClientId'], AuthFlow='USER_PASSWORD_AUTH',
            AuthParameters={'USERNAME': email, 'PASSWORD': password})['AuthenticationResult']['IdToken']
        report = {'propertyId': property_id, 'totalDamage': 20, 'totalCompensation': 10, 'actualPayout': 5,
                  'currentPortfolio': {'GOLD': 5}, 'status': 'REPORTED', 'reportGeneratedAt': '2026-01-01T00:00:00Z'}
        table.put_item(Item={**report_key, 'propertyId': property_id, 'version': 1, 'stateJson': '{}',
            'reportJson': json.dumps(report), 'status': 'REPORTED', 'assessmentId': str(uuid.uuid4()),
            'updatedAt': '2026-01-01T00:00:00Z', 'gsiPk': 'REPORTS', 'gsiSk': property_id})
        base = outputs['ApiUrl']; path = '/reports/' + property_id + '/exports'; idem = uuid.uuid4().hex
        assert request(base, 'POST', path)[0] == 401, 'JWT was not required'
        assert request(base, 'POST', path, token, idem)[0] == 403, 'Property grant was not required'
        access_key = {'pk': 'ACCESS#' + sub, 'sk': 'PROPERTY#' + property_id}
        table.put_item(Item=access_key)
        status, body = request(base, 'POST', path, token, idem)
        assert status == 202, (status, body)
        job_id = body['jobId']
        assert request(base, 'POST', path, token, idem)[1]['jobId'] == job_id, 'Duplicate request created another job'
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            status, job = request(base, 'GET', '/exports/' + job_id, token)
            assert status == 200, (status, job)
            if job['status'] == 'FAILED': raise AssertionError('Export workflow failed')
            if job['status'] == 'SUCCEEDED': break
            time.sleep(2)
        else: raise AssertionError('Export timeout')
        status, result = request(base, 'GET', '/exports/' + job_id + '/download', token)
        assert status == 200, (status, result)
        with urllib.request.urlopen(result['url'], timeout=15) as res:
            assert json.loads(res.read()) == report, 'Export contents differ from frozen report'
        object_key = jobs.get_item(Key={'pk': 'JOB#' + job_id, 'sk': 'META'}, ConsistentRead=True)['Item']['objectKey']
        print('PASS: JWT, property grant, idempotent request, workflow completion and private S3 download')
    finally:
        table.delete_item(Key=report_key)
        if access_key: table.delete_item(Key=access_key)
        if job_id:
            # Only clean jobs after completion. Leave a failed job for diagnosis/redrive.
            current = jobs.get_item(Key={'pk': 'JOB#' + job_id, 'sk': 'META'}, ConsistentRead=True).get('Item', {})
            if current.get('status') == 'SUCCEEDED':
                object_key = current.get('objectKey', object_key)
                jobs.delete_item(Key={'pk': 'JOB#' + job_id, 'sk': 'META'})
                jobs.delete_item(Key={'pk': 'OUTBOX#' + job_id, 'sk': 'EXPORT'})
        if object_key:
            versions = s3.list_object_versions(Bucket=outputs['ExportBucket'], Prefix=object_key)
            objects = [{'Key': x['Key'], 'VersionId': x['VersionId']} for kind in ['Versions', 'DeleteMarkers']
                       for x in versions.get(kind, []) if x['Key'] == object_key]
            if objects: s3.delete_objects(Bucket=outputs['ExportBucket'], Delete={'Objects': objects})
        try: cognito.admin_delete_user(UserPoolId=outputs['UserPoolId'], Username=email)
        except cognito.exceptions.UserNotFoundException: pass


if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--stack', required=True)
    main(parser.parse_args().stack)
