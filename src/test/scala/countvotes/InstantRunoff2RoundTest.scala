
import countvotes.methods.InstantRunoff2Round
import countvotes.parsers.{CandidatesParser, PreferencesParser}
import countvotes.structures.Candidate
import org.specs2.mutable.Specification

/**
  * Created by deepeshpandey on 03/06/17.
  */
class InstantRunoff2RoundTest extends Specification {

  val expectedRunoff2RoundMethodWinnerList = List(Candidate("icecream"))

  "Runoff2Round Test " should {

    "verify result" in { Runoff2RoundMethodVerification("17-example.txt", "17-candidates.txt") shouldEqual expectedRunoff2RoundMethodWinnerList }
  }

  def Runoff2RoundMethodVerification(electionFile: String, candidatesFile: String): List[Candidate] = {

    val candidates = CandidatesParser.read("../Agora/files/Examples/" + candidatesFile)
    val election =  PreferencesParser.read("../Agora/files/Examples/" + electionFile)

    InstantRunoff2Round.winners(election, candidates, 1).map {_._1}
  }
}