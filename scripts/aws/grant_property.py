"""Operator-only grant/revoke. Use the Cognito sub, never email or clientId as subject."""
import argparse
import uuid
import boto3
p=argparse.ArgumentParser()
p.add_argument('--table',required=True);p.add_argument('--subject',required=True)
p.add_argument('--property',required=True);p.add_argument('--revoke',action='store_true')
a=p.parse_args();uuid.UUID(a.property);uuid.UUID(a.subject)
t=boto3.resource('dynamodb').Table(a.table)
key={'pk':'ACCESS#'+a.subject,'sk':'PROPERTY#'+a.property}
if a.revoke:t.delete_item(Key=key)
else:t.put_item(Item=key)
print('Grant revoked' if a.revoke else 'Grant created')
