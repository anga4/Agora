package countvotes

import countvotes.methods.ContingentMethod
import countvotes.parsers.{CandidatesParser, PreferencesParser}
import countvotes.structures.Candidate
import org.specs2.mutable.Specification

//To verify tests using example from Wikipedia - https://en.wikipedia.org/wiki/Contingent_vote#Example_I

class ContingentTest extends Specification{

  val expectedContingentWinner = List(Candidate("Catherine"))

  "Contingent Test " should {

    "verify result" in { contingentMethodVerification("15-example.txt") shouldEqual expectedContingentWinner }
  }

  def contingentMethodVerification(electionFile: String): List[Candidate] = {

    val candidates = CandidatesParser.read("../Agora/files/Examples/15-candidates.txt")
    val election =  PreferencesParser.read("../Agora/files/Examples/" + electionFile)

    ContingentMethod.winners(election, candidates, 1).map {_._1}
  }
}
