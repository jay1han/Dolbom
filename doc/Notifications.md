# Notification fields

We want to filter the notification at a very high grain, while making the UI simple to use.
There are 3 levels of filtering.

1. By package name

    This represents the app in its entirety.
    At this level, you can have one indicator representing any notification posted by the app.

2. By channel ID

    We can only retrieve the channel ID (not its name), but this is useful for some apps,
    and matches what's shown in the notification setting screen.
    
3. Other fields

    There are many text fields associated with notifications, and the semantics are
    mostly related to the presentation, not the content.
    However, we group these fields in 4 categories to make the filtering more useful.
    
    | Category | Notification extras                                              |
    |----------|------------------------------------------------------------------|
    | Title    | `EXTRA_TITLE`<br>`EXTRA_CONVERSATION_TITLE`<br>`EXTRA_TITLE_BIG` |
    | Subtitle | `EXTRA_SUB_TEXT`<br>`EXTRA_PEOPLE_LIST`                          |
    | Text     | `EXTRA_INFO_TEXT`<br>`EXTRA_SUMMARY_TEXT`<br>`EXTRA_TEXT`        |
    | Long     | `EXTRA_BIG_TEXT`<br>`EXTRA_TEXT_LINES`                           |

Here are some reverse-engineered utilizations for a few popular apps.

| App              | Channel ID | Title                              | Subtitle   | Text                               | Long                       |
|------------------|------------|------------------------------------|------------|------------------------------------|----------------------------|
| Gmail            | Account    | Sender name                        | Account    | Subject                            | Full text                  |
| Messaging        |            | Sender                             | SIM slot   | (empty)                            | Full text                  |
| WhatsApp (Group) | Not useful | Group name with and withour sender | (empty)    | Full text, with and without sender |                            |
| KakaoTalk        |            | Sender name                        | Group name | Full text                          |                            |
| Outlook          | Account    | Sender name                        | Account    | Subject and text                   | Subject and text (Unicode) |

Ignoring a specific configuration means that no indicator will be shown.
You can also mix ignored notifications at some levels with
active ones at other levels.
For example, you can ignore all Gmail notifications except those
on a specific account:

- Set the general Gmail configuration to "Ignore"

- Set the specific Subtitle (account address) to a specific letter

You can also do the opposite, to ignore specific configurations
while allowing the general one to activate in other cases.
