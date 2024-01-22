### ToDo list

| priority | issue                                                                                  |
|----------|----------------------------------------------------------------------------------------|
| critical | delay match finish until photo received                                                |
| critical | spread public commands prohibition to all kinds of commands                            |
| critical | rating                                                                                 |
| critical | match finish check should exclude 5+ players from check when 4 already submitted votes |
| critical | recheck validation conditions and orders in TelegramUpdateProcessor                    |
| high     | simultaneous matches spam control                                                      |
| high     | match submits throttling                                                               |
| high     | add leaders selection flow                                                             |
| high     | leaders rating                                                                         |
| medium   | add menu buttons (SetMyCommands)                                                       |
| medium   | test TelegramApiException to runtime handling                                          |
| medium   | test TelegramUpdateProcessor unknown exceptions handling for both methods              |
| medium   | add TelegramUpdateProcessor#sendUserNotificationMessage null replyId scenario          |
| medium   | handle infra errors in MessagingService callbacks                                      |
| low      | add processor selection test for TelegramUpdateProcessor                               |
| low      | move settings to separate repo, add settings command                                   |
| low      | move settings to separate repo, add settings command                                   |
| low      | consider making all commands private chats only                                        |
| low      | check threadId usage in telegram                                                       |
| low      | check whether validation is required for TelegramUpdateProcessor pollAnswer updates    |
| low      | add runtime validator selection to TelegramUpdateProcessor                             |

### Known bugs

| ID | issue                                | offer                                        |
|----|--------------------------------------|----------------------------------------------|
| 1  | new and register produces same error | replace message with abstract command + args |
