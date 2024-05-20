# ToDo list

| priority | issue                                                                               |
|----------|-------------------------------------------------------------------------------------|
| high     | match submits throttling                                                            |
| high     | leaders rating                                                                      |
| medium   | multiple on-submit matches                                                          |
| medium   | simultaneous matches spam control                                                   |
| medium   | test TelegramUpdateProcessor unknown exceptions handling for both methods           |
| low      | add processor selection test for TelegramUpdateProcessor                            |
| low      | check whether validation is required for TelegramUpdateProcessor pollAnswer updates |
| low      | add runtime validator selection to TelegramUpdateProcessor                          |

# Design considerations

### @Transactional vs TransactionalTemplate

A lot of methods in the project are executed asynchronously and use many network IO calls, so most of project's code
uses TransactionalTemplate for transactional execution instead of annotation-based transactions to ensure transactions
durations are as minimal as possible and do not include any side IO calls.
