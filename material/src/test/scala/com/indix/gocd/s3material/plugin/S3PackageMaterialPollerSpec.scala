package com.indix.gocd.s3material.plugin

package material.plugin.config

import java.util.Date

import com.amazonaws.services.s3.AmazonS3Client
import com.google.gson.GsonBuilder
import com.indix.gocd.models.{Artifact, Revision, RevisionStatus}
import com.indix.gocd.utils.store.S3ArtifactStore
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar

class S3PackageMaterialPollerSpec extends FlatSpec with MockitoSugar with org.scalatest.Matchers  {
  behavior of "S3PackageMaterialPoller"

  val sut = spy(new S3PackageMaterialPoller())
  val mockS3ArtifactStore = mock[S3ArtifactStore]
  doReturn(mockS3ArtifactStore).when(sut).s3ArtifactStore("S3 Bucket")

  private def getRequest(name: String, body: String) = {
    new GoPluginApiRequest {

      override def requestParameters() = ???

      override def extensionVersion() = ???

      override def extension() = ???

      override def requestHeaders() = ???

      override def requestBody() = body


      override def requestName() = name
    }
  }

  it should "return null result if no new revision since previous revision" in {
    val status = new RevisionStatus(new Revision("1.1"), new Date(), "", "", "")
    doReturn(status).when(mockS3ArtifactStore).getLatest(Matchers.any[AmazonS3Client], Matchers.any[Artifact])
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_LATEST_REVISION_SINCE, """
                  |{
                  |    "repository-configuration": {
                  |        "S3_BUCKET": {
                  |            "value": "S3 Bucket"
                  |        }
                  |    },
                  |    "package-configuration": {
                  |        "PIPELINE_NAME": {
                  |            "value": "Pipeline"
                  |        },
                  |        "STAGE_NAME": {
                  |            "value": "Stage"
                  |        },
                  |        "JOB_NAME": {
                  |            "value": "Job"
                  |        }
                  |    },
                  |    "previous-revision": {
                  |        "revision": "1.1",
                  |        "timestamp": ""
                  |    }
                  |}
                """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[AnyRef](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap == null)
  }

  it should "get more latest revision since previous revision" in {
    val status = new RevisionStatus(new Revision("1.2"), new Date(), "", "", "")
    doReturn(status).when(mockS3ArtifactStore).getLatest(Matchers.any[AmazonS3Client], Matchers.any[Artifact])
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_LATEST_REVISION_SINCE, """
                  |{
                  |    "repository-configuration": {
                  |        "S3_BUCKET": {
                  |            "value": "S3 Bucket"
                  |        }
                  |    },
                  |    "package-configuration": {
                  |        "PIPELINE_NAME": {
                  |            "value": "Pipeline"
                  |        },
                  |        "STAGE_NAME": {
                  |            "value": "Stage"
                  |        },
                  |        "JOB_NAME": {
                  |            "value": "Job"
                  |        }
                  |    },
                  |    "previous-revision": {
                  |        "revision": "1.1",
                  |        "timestamp": ""
                  |    }
                  |}
                """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, AnyRef]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.get("revision") == "1.2")
  }

  it should "get latest revision" in {
    val status = new RevisionStatus(new Revision("1.1"), new Date(), "", "", "")
    doReturn(status).when(mockS3ArtifactStore).getLatest(Matchers.any[AmazonS3Client], Matchers.any[Artifact])
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_LATEST_REVISION, """
                  |{
                  |    "repository-configuration": {
                  |        "S3_BUCKET": {
                  |            "value": "S3 Bucket"
                  |        }
                  |    },
                  |    "package-configuration": {
                  |        "PIPELINE_NAME": {
                  |            "value": "Pipeline"
                  |        },
                  |        "STAGE_NAME": {
                  |            "value": "Stage"
                  |        },
                  |        "JOB_NAME": {
                  |            "value": "Job"
                  |        }
                  |    }
                  |}
                """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, AnyRef]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.get("revision") == "1.1")
  }

  it should "return success when package is found" in {
    doReturn(true).when(mockS3ArtifactStore).exists("S3 Bucket", "Pipeline/Stage/Job/")
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_CHECK_PACKAGE_CONNECTION, """
                  |{
                  |    "repository-configuration": {
                  |        "S3_BUCKET": {
                  |            "value": "S3 Bucket"
                  |        }
                  |    },
                  |    "package-configuration": {
                  |        "PIPELINE_NAME": {
                  |            "value": "Pipeline"
                  |        },
                  |        "STAGE_NAME": {
                  |            "value": "Stage"
                  |        },
                  |        "JOB_NAME": {
                  |            "value": "Job"
                  |        }
                  |    }
                  |}
                """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, String]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.get("status") == "success")
  }

  it should "return failure when package is not found" in {
    doReturn(false).when(mockS3ArtifactStore).exists("S3 Bucket", "Pipeline/Stage/Job/")
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_CHECK_PACKAGE_CONNECTION, """
                  |{
                  |    "repository-configuration": {
                  |        "S3_BUCKET": {
                  |            "value": "S3 Bucket"
                  |        }
                  |    },
                  |    "package-configuration": {
                  |        "PIPELINE_NAME": {
                  |            "value": "Pipeline"
                  |        },
                  |        "STAGE_NAME": {
                  |            "value": "Stage"
                  |        },
                  |        "JOB_NAME": {
                  |            "value": "Job"
                  |        }
                  |    }
                  |}
                """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, String]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.get("status") == "failure")
  }

  it should "return success on package configuration validated" in {
    doReturn(true).when(mockS3ArtifactStore).bucketExists()
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_VALIDATE_PACKAGE_CONFIGURATION, """
                                         |{
                                         |    "package-configuration": {
                                         |
                                         |    }
                                         |}
                                       """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.List[AnyRef]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.isEmpty)
  }

  it should "return success on repository configuration validated" in {
    doReturn(true).when(mockS3ArtifactStore).bucketExists()
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_VALIDATE_REPOSITORY_CONFIGURATION, """
                                         |{
                                         |    "repository-configuration": {
                                         |        "S3_BUCKET": {
                                         |            "value": "S3 Bucket"
                                         |        }
                                         |    }
                                         |}
                                       """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.List[AnyRef]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.isEmpty)
  }

  it should "return validation errors on repository configuration invalid" in {
    doReturn(true).when(mockS3ArtifactStore).bucketExists()
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_VALIDATE_REPOSITORY_CONFIGURATION, """
                                         |{
                                         |    "repository-configuration": {
                                         |        "S3_BUCKET": {
                                         |            "value": ""
                                         |        }
                                         |    }
                                         |}
                                       """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.List[java.util.Map[String, String]]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(!resultMap.isEmpty)
    assert(resultMap.get(0).get("key") == "S3_BUCKET")
    assert(resultMap.get(0).get("message") == "S3 bucket must be specified")
  }

  it should "return success when repository is found" in {
    doReturn(true).when(mockS3ArtifactStore).bucketExists()
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_CHECK_REPOSITORY_CONNECTION, """
                                         |{
                                         |    "repository-configuration": {
                                         |        "S3_BUCKET": {
                                         |            "value": "S3 Bucket"
                                         |        }
                                         |    }
                                         |}
                                       """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, String]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.get("status") == "success")
  }

  it should "return failure when repository is not found" in {
    doReturn(false).when(mockS3ArtifactStore).bucketExists()
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_CHECK_REPOSITORY_CONNECTION, """
                                         |{
                                         |    "repository-configuration": {
                                         |        "S3_BUCKET": {
                                         |            "value": "S3 Bucket"
                                         |        }
                                         |    }
                                         |}
                                       """.stripMargin))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, String]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    assert(resultMap.get("status") == "failure")
  }

  it should "return repository configuration" in {
    doReturn(false).when(mockS3ArtifactStore).bucketExists()
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_REPOSITORY_CONFIGURATION, ""))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, AnyRef]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    resultMap.keySet() should contain only "S3_BUCKET"
  }

  it should "return package configuration" in {
    doReturn(false).when(mockS3ArtifactStore).bucketExists()
    val result = sut.handle(getRequest(S3PackageMaterialPoller.REQUEST_PACKAGE_CONFIGURATION, ""))
    val resultMap = new GsonBuilder().create.fromJson[java.util.Map[String, AnyRef]](result.responseBody(), classOf[Any])
    assert(result.responseCode() == 200)
    resultMap.keySet() should contain only ("PIPELINE_NAME", "STAGE_NAME", "JOB_NAME")
  }

}
