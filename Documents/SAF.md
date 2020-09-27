# Storage Access Framework

Starting with the KitKat version of Android, Google has implemented
a new framework for access to files instead of the classical
Linux file system.

The old framework could still be used to read and write local shared
storage. But to write to the removable storage the new framework
is needed for recent Android versions.  
The SAF (Storage Access Framework) imply an interaction between
the user and the system on the side of the application: this would
disrupt the flow of screens specific to the application.  
It is also not possible to completely adopt the new framework
for the application and to keep the compatibility with older
Android versions.

Thus, the SAF is presently not used for Vtrk. The implication is
that it is often not possible to store new data on removable storage.

Google documentation on the [SAF](https://developer.android.com/training/data-storage/shared/documents-files).

