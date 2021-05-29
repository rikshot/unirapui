#!/usr/bin/env bash

set -x
set -e

# Generate CA key and self-signed cert
openssl genrsa -out ca.key
openssl req -new -x509 -sha256 -key ca.key -out ca.crt -subj "/C=FI/ST=Uusimaa/L=Vantaa/O=Unirapui/OU=CA/CN=127.0.0.1/emailAddress=ville@orkas.fi"

# Generate server key and cert signed with CA
openssl genrsa -out server.key
openssl req -new -sha256 -key server.key -out server.csr -subj "/C=FI/ST=Uusimaa/L=Vantaa/O=Unirapui/OU=Server/CN=127.0.0.1/emailAddress=ville@orkas.fi"
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 365 -sha256 -extfile server.cnf

# Generate client key and cert signed with CA
openssl genrsa -out client.key
openssl req -new -sha256 -key client.key -out client.csr -subj "/C=FI/ST=Uusimaa/L=Vantaa/O=Unirapui/OU=Client/CN=127.0.0.1/emailAddress=ville@orkas.fi"
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -days 365 -sha256 -extfile client.cnf

# Cleanup
rm ca.srl client.csr server.csr