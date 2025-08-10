# Tyre-Stock-Checker
(2025) Android app developed for (and currently being used by) a garage to make stock checking tyres easier - built with Android Studio and Java.

It provides importing of .xlsx or .csv files from GDS containing stock information, and displays all tyres in a scrollable spreadsheet view, allowing for searching, sorting, editing, and printing a spreadsheet with all changes visible to PDF or paper.

## Technologies/Techniques Used:
- Android Studio with Java, using Activities, Fragments, Intents, Parcelables, and Resources
- Parsing of .xlsx (excel spreadsheet) and .csv files
- Android Room with SQL for persistent storage
- Lazy loading of fragments for performance and to reduce memory usage

## Features:
- Importing of .xlsx and .csv files
- Saving and loading of changes
- Scrollable spreadsheet view of tyre information, with resizable column widths through dragging
- Part number and general search fields, and sorting by part number, last sold date, and stock (asc and desc)
- Easy adding and subtracting from a 'seen' count for each tyre
- Editing of all fields for each tyre, with an extra comment field available; insertions and deletions are clearly marked
- Tyres can be reversibly marked as done to prevent further editing
- A spreadsheet view of all tyres can be printed to PDF or paper, clearly displaying changes to tyre information, for updating the central stock database
- Option to hide out of stock tyres
- Button to count number of tyres seen
- Option to add new tyres (which are deletable)
