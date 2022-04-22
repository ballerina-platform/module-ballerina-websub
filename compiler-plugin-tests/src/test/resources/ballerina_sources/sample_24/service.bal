import ballerina/websub;

service /foo on new websub:Listener(9090) {}
