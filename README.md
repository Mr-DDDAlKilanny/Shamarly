# Shamarly
Shamarly Mushaf for Android مصحف الشمرلي للأندرويد

َQuran Mushaf implementation for Android (Shamraly Print)


# Features:
* Tafseer (up to 9 tafseer books)

* Listen to recites

* Download Manager for recites, with alternative servers

* View single page / dual pages

* Ability to show borders for page

* Support for night mode, and changing font/background colors

* Support for making Muhaf font bold (through image-processing dilation)

* Support for zooming Mushaf image and rotation

* Support for selecting single Ayah from page


# Code overview
Most functional code in in MainActivity.java. Method MainActivity.readPage is responsible about reading a page image and processing it. Method MainActivity.playRecite is playing recites.
Rest of functions are in other activities.
Class Utils contains a lot of helper methods for application, such as download and file IO.


# Project uses:
* Quran pdf (from https://quraankarem.wordpress.com/shamarly), turned into images (using PdfFill https://www.pdfill.com), and compressed images (using tinypng.com)

* Ayah Detection project (https://github.com/quran/ayah-detection) for detecting ayah circles in pages

* Every Ayah (everyayah.com), yaquran.com for recitations

* Quran Text for searching, Quran data and Tafseer from  (http://tanzil.net/download/)

* Android Directory Chooser Project (https://github.com/passy/Android-DirectoryChooser)

* Others