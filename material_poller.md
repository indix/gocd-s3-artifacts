Material Poller
===

This is a package repository plugin that polls S3 for artifacts and triggers the pipeline as appropriate.

The package repository can be configured by heading to `Admin -> Package Repositories`

In the `Add Package Repository` form, provide a name for the S3 package repository. This is a unique name to identify the S3 package repository.

Next, choose `s3material` as the `Type` of the package repository. This should bring up a text box to enter the S3 bucket name. Enter the name of the bucket where you want to store the artifacts in this field.

Use the `Check Connection` button to verify that the credentials are fine and the provided bucket exists. This state is shown in the following screen![](material_configuration.png)shot:

