package agora.votecounter

import agora.parser.{CandidatesParser, ParameterParser, PreferencesParser}
import agora.model.Candidate
import org.specs2.mutable.Specification

/**
  * Created by deepeshpandey on 06/08/17.
  */
class SMCMethodTest extends Specification{

  val smcWinnerList = List(Candidate("C"))

  "SMC Test " should {

    "verify result" in { smcMethodVerification("37-example.e", "37-candidates.txt", "method-param1.json") shouldEqual smcWinnerList }
    "verify result" in { smcMethodVerification("37-example.e", "37-candidates.txt", "method-param.json") shouldEqual smcWinnerList }
  }

  def smcMethodVerification(electionFile: String, candidateFile: String, paramFile: String): List[Candidate] = {

    val dir = "../Agora/files/Examples/"
    val candidates = CandidatesParser.read(dir + candidateFile)
    val election =  PreferencesParser.read(dir + electionFile)
    val param = ParameterParser.parse(dir + paramFile)

    SMC.smcWinner(election, candidates, param, 1).map {_._1}
  }


}
