   49  aws ec2 create-vpc --cidr-block '192.168.0.0/23' --tag-specifications "ResourceType=vpc,Tags=[{Key=Name,Value=my-study-vpc}]"
   50  aws ec2 create-subnet --cidr-block '192.168.0.0/26'  --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=my-study-publics-1}]" --vpc-id vpc-05d3edef7812d8814
   51  aws ec2 create-subnet --cidr-block '192.168.0.64/26'  --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=my-study-publics-2}]" --vpc-id vpc-05d3edef7812d8814 --availability-zone 'ap-south-1b'
   52   aws ec2 create-internet-gateway  --tag-specifications "ResourceType=subnet,Tags=[
   53  aws ec2 create-internet-gateway --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=my-study-igw}]"
   54  aws ec2 create-internet-gateway --tag-specifications "ResourceType=internet-gateway,Tags=[{Key=Name,Value=my-study-igw}]"
   55  aws ec2 attach-internet-gateway --internet-gateway-id  "igw-0ea3cfff9e839b885" --vpc-id vpc-05d3edef7812d8814