"""Explicit operator recovery for Streams age-out. Sends existing PENDING export outboxes.
Scanning is acceptable for this recovery tool, not for dashboard polling.
"""
import argparse,json
import boto3
from boto3.dynamodb.conditions import Attr
p=argparse.ArgumentParser();p.add_argument('--table',required=True);p.add_argument('--queue-url',required=True)
a=p.parse_args();table=boto3.resource('dynamodb').Table(a.table);sqs=boto3.client('sqs');start=None;count=0
while True:
    args={'FilterExpression':Attr('pk').begins_with('OUTBOX#') & Attr('status').eq('PENDING')}
    if start:args['ExclusiveStartKey']=start
    page=table.scan(**args)
    for item in page['Items']:
        sqs.send_message(QueueUrl=a.queue_url,MessageBody=json.dumps({'jobId':item['jobId']}))
        table.update_item(Key={'pk':item['pk'],'sk':item['sk']},UpdateExpression='SET #s=:s',
                          ExpressionAttributeNames={'#s':'status'},ExpressionAttributeValues={':s':'SENT'})
        count+=1
    start=page.get('LastEvaluatedKey')
    if not start:break
print('Dispatched',count,'outboxes')
