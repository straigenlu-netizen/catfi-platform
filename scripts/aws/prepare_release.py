"""Bake a literal Lambda version identity: SAM must notice parameter-only configuration changes."""
import argparse, hashlib, json, os
from pathlib import Path
import yaml
p=argparse.ArgumentParser();p.add_argument('--release',required=True);p.add_argument('--output',required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[2]
template=yaml.safe_load((root/'infra/aws/template.yaml').read_text())
config={key:os.environ.get(key,'') for key in ['EMAIL_FROM','EMAIL_TO','WEATHER','SNAPSTART']}
artifact=root/'services/aws-functions/target/catfi-functions.jar'
if not artifact.is_file():raise SystemExit('Build aws-functions before preparing a release')
identity=hashlib.sha256(artifact.read_bytes()+a.release.encode()+json.dumps(config,sort_keys=True).encode()).hexdigest()
for resource in template['Resources'].values():
    if resource['Type']=='AWS::Serverless::Function':resource['Properties']['AutoPublishCodeSha256']=identity
# The generated template can be placed anywhere by callers; the artifact location remains exact.
template['Globals']['Function']['CodeUri']=str(artifact)
Path(a.output).write_text(yaml.safe_dump(template,sort_keys=False))
print('Prepared Lambda release template')
