# ToDo list

| priority | issue                                                                               |
|----------|-------------------------------------------------------------------------------------|
| high     | add maintenance windows                                                             |
| high     | match submits throttling                                                            |
| high     | leaders rating                                                                      |
| medium   | multiple on-submit matches                                                          |
| medium   | simultaneous matches spam control                                                   |
| medium   | test TelegramApiException to runtime handling                                       |
| medium   | test TelegramUpdateProcessor unknown exceptions handling for both methods           |
| medium   | add TelegramUpdateProcessor#sendUserNotificationMessage null replyId scenario       |
| medium   | handle infra errors in MessagingService callbacks                                   |
| low      | add processor selection test for TelegramUpdateProcessor                            |
| low      | check whether validation is required for TelegramUpdateProcessor pollAnswer updates |
| low      | add runtime validator selection to TelegramUpdateProcessor                          |
| low      | check all callbacks thrown                                                          |

# Design considerations

### @Transactional vs TransactionalTemplate

A lot of methods in the project are executed asynchronously and use many network IO calls, so most of project's code
uses TransactionalTemplate for transactional execution instead of annotation-based transactions to ensure transactions
durations are as minimal as possible and do not include any side IO calls.
