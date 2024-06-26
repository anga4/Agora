package agora.votecounter

import com.typesafe.scalalogging.LazyLogging
import agora.model._
import agora.model.{PreferenceBallot => Ballot}
import agora.votecounter.common.PreferencePairwiseComparison

import spire.math.Rational

/**
  * Created by deepeshpandey on 03/06/17.
  * About : Sequential Majority Comparison (SMC): Fix some enumeration {x1, x2, . . . , xm} of the
  * alternatives. The winner of round 1 is x1; the winner of round i + 1 is the winner w of
  * round i, if w >(majority) xi+1, and is xi+1, if xi+1 >(majority) w; and the ultimate winner is the winner
  * of round m.
  */
object SMC extends VoteCounter[Ballot] with PreferencePairwiseComparison with LazyLogging {

  def runVoteCounter(election: Election[Ballot], candidates: List[Candidate], param: Parameters, numVacancies: Int): Report[Ballot] = {

    val result: Result = new Result
    val report: Report[Ballot] = new Report[Ballot]
    
    print("\n INPUT ELECTION: \n")
    //printElection(election)

    report.setCandidates(candidates)

    report.setWinners(smcWinner(election, candidates, param, numVacancies))

    report
  }

  def smcWinner(election: Election[Ballot], ccandidates: List[Candidate], param: Parameters, numVacancies: Int):
  List[(Candidate, Rational)] = {

    // it may be possible that param candidates and actual candidates are inconsistent
    require(param.comparisonOrder.isDefined && param.comparisonOrder.get.forall(c => ccandidates.exists(cand => cand.name == c)))

    val zeroRational = Rational(0, 1)
    val majorityRational = Rational(1, 2)

    val totalVoters = election.weight
    val electionResponse = pairwiseComparison(election, ccandidates)

    // generate the ordered list of candidates
    val candOrderList = param.comparisonOrder.get.map(name => ccandidates.find(cand => cand.name == name).get)

    List((candOrderList.head /: candOrderList.tail)((cA, cB) => {
      if (electionResponse(ccandidates.indexOf(cA))(ccandidates.indexOf(cB)) > majorityRational * totalVoters) cA else cB
    })).map(c => (c, zeroRational))

  }

  override def winners(e: Election[Ballot], ccandidates: List[Candidate], numVacancies: Int): List[(Candidate, Rational)] = ???
}
