# ToDo list

| priority | issue                                                                               |
|----------|-------------------------------------------------------------------------------------|
| critical | rating                                                                              |
| critical | recheck validation conditions and orders in TelegramUpdateProcessor                 |
| critical | after match start revocation                                                        |
| critical | finish match after screenshot upload                                                |
| high     | simultaneous matches spam control                                                   |
| high     | match submits throttling                                                            |
| high     | add leaders selection flow                                                          |
| high     | leaders rating                                                                      |
| medium   | add menu buttons (SetMyCommands)                                                    |
| medium   | test TelegramApiException to runtime handling                                       |
| medium   | test TelegramUpdateProcessor unknown exceptions handling for both methods           |
| medium   | add TelegramUpdateProcessor#sendUserNotificationMessage null replyId scenario       |
| medium   | handle infra errors in MessagingService callbacks                                   |
| low      | add processor selection test for TelegramUpdateProcessor                            |
| low      | move settings to separate repo, add settings command                                |
| low      | move settings to separate repo, add settings command                                |
| low      | consider making all commands private chats only                                     |
| low      | check threadId usage in telegram                                                    |
| low      | check whether validation is required for TelegramUpdateProcessor pollAnswer updates |
| low      | add runtime validator selection to TelegramUpdateProcessor                          |
| low      | add logging                                                                         |
| low      | check all callbacks thrown                                                          |

# Known bugs

| ID | issue                                | offer                                        |
|----|--------------------------------------|----------------------------------------------|
| 1  | new and register produces same error | replace message with abstract command + args |

# Design considerations

### @Transactional vs TransactionalTemplate

A lot of methods in the project are executed asynchronously and use many network IO calls, so most of project's code
uses TransactionalTemplate for transactional execution instead of annotation-based transactions to ensure transactions
durations are as minimal as possible and do not include any side IO calls.

### equals() and hashCode()
