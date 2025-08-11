## 3.2.13 (2025-08-11)
* Update project
* Update libs to handle 16KB pages

## 3.2.11 (2025-03-11)
* Fix issues when swiping PDfs in a ViewPager
* Convert build.gradle to kts and use version catalog
* Update libs

## 3.2.10 (2024-06-28)
* Add setThumbnailRatio to change the render quality of thumbnails
* Update kotlin version to 2.0.0
* Update gradle to 8.8
* Upgrade AGP to 8.5.0

## 3.2.9 (2024-05-14)
* Add loadPagesForPrinting method on PDFView to start generating bitmaps for printing
* Add OnReadyForPrintingListener listener to know when bitmaps are ready

## 3.2.8 (2024-01-09)
* Add the possibility to have space above the first page and below the last page of the PDF
* Change the default min, mid, max zoom value
* Change the initial position in the PDF in order to take into account that first space
* Change the length of the document to take into account the first and last spacer if any

## 3.2.7 (2024-01-02)
* Change the way we declare the dependency in order to be able to use the exception created in 
  application that uses this library

## 3.2.6 (2024-01-02)
* Add onAttach and onDetach listeners

## 3.2.4 (2023-12-26)
* Add a minimum value before triggering the touch priority
* Fix some warnings

## 3.2.3 (2023-12-07)
* Add the possibility to customize the page handle
