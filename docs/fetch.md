# Fetch

`Fetch from S3` is a task plugin, that, well, fetches artifacts from S3.

## Environment variables

There are no required environment variables for this plugin.

The fetch plugin gets the bucket and package information from the environment variables injected by the material plugin

## Configuration

Once the plugin is installed, it should be available as a task, ready to be added into any of your jobs. From the admin section of the concerned job, click on the `Add new task` link and choose `Fetch S3 package` as the task

In the `Add new task - Fetch S3 package` modal, we need to specify the `Repository name` and `Package name`. The plugin will fetch the appropriate artifacts for the current run of the pipeline by using the information from the material plugin.

The `Destination directory` for artifacts needs to be specified as well. An example configuration for the fetch plugin is shown in the following screenshot:

![](resources/images/modal_fetch.png)

Click `Save` to add the task to the job.