package material.store

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers._

class RevisionSpec extends FlatSpec{
  "Revision" should "honor compare method when applying ordering" in {
    List(Revision("1.2.200"), Revision("0.5.102"), Revision("0.5.101")).min should be(Revision("0.5.101"))
    List(Revision("1.2.200"), Revision("0.5.102"), Revision("0.5.101")).max should be(Revision("1.2.200"))
    List(Revision("1.2"), Revision("1.2.1"), Revision("0.5")).max should be(Revision("1.2.1"))
  }
}
