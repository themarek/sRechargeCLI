# sRechargeCLI

This is a CLI tool to start/stop the loading of your ShellRecharge / NewMotion wallbox. 
Inspired by https://github.com/Siumba/NewMotionAPI this is a port to Kotlin.

1. gradle build
2. create a config.yaml
3. java -jar  build/libs/sRecharge-1.0-SNAPSHOT-all.jar info

config.yaml (yes, it needs to contain all the lines):
```
!!Configuration
chargerId: "run the tool with info parameter -> It is the uuid of chargePoints"
card: "it is the rfid of ChargeToken (see previous parameter)"
username: "the username you are using to login"
password: "the password you are using to login"
````
