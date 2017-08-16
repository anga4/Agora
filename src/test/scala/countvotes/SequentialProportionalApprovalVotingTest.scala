import countvotes.methods.SequentialProportionalApprovalVoting
import countvotes.parsers.{CandidatesParser, PreferencesParser}
import countvotes.structures.{Candidate,Rational}
import org.specs2.mutable.Specification


class SequentialProportionalApprovalVotingTest extends Specification {

  val expectedSequentialProportionalApprovalWinnerList = List((Candidate("D"), Rational(3,1)), (Candidate("B"), Rational(4,1)),  (Candidate("A"), Rational(8,1)))

  "SequentialProportionalApprovalVoting Test " should {

    "verify result" in { SequentialProportionalApprovalVotingVerification("36-example.e", "36-candidates.txt") shouldEqual expectedSequentialProportionalApprovalWinnerList}
  }

  def SequentialProportionalApprovalVotingVerification(electionFile: String, candidatesFile: String): List[(Candidate,Rational)] = {

    val candidates = CandidatesParser.read("../Agora/files/Examples/" + candidatesFile)
    val election =  PreferencesParser.read("../Agora/files/Examples/" + electionFile)

    SequentialProportionalApprovalVoting.winners(election, candidates, 3)
  }
}
