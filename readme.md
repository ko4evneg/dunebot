### ToDo list

| priority | issue                                                                                  |
|----------|----------------------------------------------------------------------------------------|
| critical | match finish check should exclude 5+ players from check when 4 already submitted votes |
| critical | recheck validation conditions and orders in TelegramUpdateProcessor                    |
| high     | simultaneous matches spam control                                                      |
| high     | match submits throttling                                                               |
| high     | add leaders selection flow                                                             |
| high     | add TelegramMessagingService tests                                                     |
| medium   | test TelegramApiException to runtime handling                                          |
| medium   | test TelegramUpdateProcessor unknown exceptions handling for both methods              |
| medium   | add TelegramUpdateProcessor#sendUserNotificationMessage null replyId scenario          |
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
