# ToDo list

| priority | issue                                                                               |
|----------|-------------------------------------------------------------------------------------|
| high     | add maintenance windows                                                             |
| high     | match submits throttling                                                            |
| high     | add leaders selection flow                                                          |
| high     | leaders rating                                                                      |
| medium   | simultaneous matches spam control                                                   |
| medium   | add menu buttons (SetMyCommands)                                                    |
| medium   | test TelegramApiException to runtime handling                                       |
| medium   | test TelegramUpdateProcessor unknown exceptions handling for both methods           |
| medium   | add TelegramUpdateProcessor#sendUserNotificationMessage null replyId scenario       |
| medium   | handle infra errors in MessagingService callbacks                                   |
| low      | replace refresh profile message with full names                                     |
| low      | transient logging ID with ThreadLocal                                               |
| low      | transient logging ID with ThreadLocal                                               |
| low      | add processor selection test for TelegramUpdateProcessor                            |
| low      | move settings to separate repo, add settings command                                |
| low      | move settings to separate repo, add settings command                                |
| low      | consider making all commands private chats only                                     |
| low      | check threadId usage in telegram                                                    |
| low      | check whether validation is required for TelegramUpdateProcessor pollAnswer updates |
| low      | add runtime validator selection to TelegramUpdateProcessor                          |
| low      | add logging                                                                         |
| low      | check all callbacks thrown                                                          |
| low      | use tempdir in save screenshot service                                              |
| low      | add admin commands for timeouts                                                     |

# Known bugs

| ID | issue                                                                 |
|----|-----------------------------------------------------------------------|
| 1  | /cancel missing feedback when no match (if match exists - check also) | 
| 2  | check screenshot saving (may be not invoked)                          |
| 3  | /register x (y) z.           (no output)                              |
| 3  | /cancel           (no output)                                         |

# Design considerations

### @Transactional vs TransactionalTemplate

A lot of methods in the project are executed asynchronously and use many network IO calls, so most of project's code
uses TransactionalTemplate for transactional execution instead of annotation-based transactions to ensure transactions
durations are as minimal as possible and do not include any side IO calls.

### equals() and hashCode()
