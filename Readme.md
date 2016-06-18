#Cash Track

This app helps keep track of your daily expenses.

##Using the App

Click the plus sign to create a new entry, then enter a description and amount, then
click the OK button. Expense items can be assigned to a category if desired.

By default the main screen displays the recent items, sorted by date. The action menu has an
option to display all items. The display items can also be filtered by category, using
the pulldown at the bottom of the screen.

Long-click an item to edit or delete it.

Use the action menu to create a new category or new user. Our user name is internal to
the app, and is unrelated to the new Android system multi-user feature. The Expense Activity
shows items only for the currrent user, and the user is remembered across app re-starts.

When adding or editing an item, assign a different user name to that item does not change
the app's idea of the current user.

Deleting a category deletes all associated expenses for the current user. Deleting a user
deletes all associated expenses and categories for that user. You get a confirmation
prompt when attempting to delete a User or Category.

Expense items (for all users) can be exported to the sdcard. The file format is
comma-separated-values, one row per item, UTF-8. The file path is "./expense/expense.csv"
on the sdcard.

##Acknowledgements

The launcher icon was created by the [Icon8 Web Site](https://icons8.com/)

This code was forked from [simeonge/expensereport](https://github.com/simeonge/expensereport)

His code served as a very useful base to make my own modifications, without having to figure
out the many android app quirks at the beginning.

##Building

- This app is built with Android Studio 2.1, Gradle 2.1 and SDK 22.
- After cloning from github, import in-place as a project (File|New|Import Project)
- I test on Android 5.1 (API 22).
- Most recent phones will require that you sign the .apk (using a key that you created.)
- I side-load the signed .apk to my phone using `adb install release-file.apk` (I had to turn
off the USB MPT mode for my phone.)

##Changes from the Fork

- this app removes the item sub-totals and totals (not useful to me)
- this app shows a date for each item
- this app allows editting user & category & date for an item
- this app supports data export
- this app has more display filtering options
- I'm experimenting with different algorithms to predict what the user is about to enter.

