# terraform block
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.0"
    }
  }
}
# Configure the AWS Provider
provider "aws" {
    region = "us-west-2"
}

# Create an EC2 instance of type linux
resource "aws_instance" "linux_server" {
  ami           = "ami-0ca285d4c2cda3300"
  instance_type = "t2.micro"
  availability_zone = "us-west-2b"
  security_groups = [ "terraform_SG" ]
  key_name = "us_west2_keypair"
  tags = {
    "Name" = "linux_server" # map of string
  }
}
# creating security group using terraform
resource "aws_security_group" "terraform_SG" {
  name        = "terraform_SG"
  description = "Allow SSH and HTTP inbound traffic"
  vpc_id      = "vpc-855d4afd"

  ingress {
    description      = "Allow HTTP traffic"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    description      = "allow SSH access"
    from_port        = 22
    to_port          = 22
    protocol         = "tcp"
    cidr_blocks      = ["73.132.129.120/32"]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "terraform_SG"
  }

}
# Create a custom VPC
resource "aws_vpc" "terraform_vpc" {
  cidr_block = "10.123.0.0/16"
  tags = {
    "Name" = "terraform_vpc"
  }
}