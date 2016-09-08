package countvotes.methods

import countvotes.structures._
import countvotes.algorithms._


import scala.collection.immutable.ListMap
import collection.mutable.{HashMap => Map}
import scala.collection.SortedMap
import collection.mutable.HashSet
import collection.breakOut
import scala.util.Random
import scala.util.Sorting
import java.io._



class SenateMethod extends STVAustralia
 with DroopQuota // Section 273 (8)
 with NoFractionInQuota // Section 273 (8)
 with NewWinnersOrderedByTotals[ACTBallot] // TODO
 with SenateSurplusDistributionTieResolution // Section 273 (22)
 with NoFractionLoss // it appears that there is no fraction loss in Senate (I have not found it in the Legislation)- see Section 273 (9)(b)
 with SenateExclusion //   exactly like ACTExclusion
 with SenateExactWinnerRemoval // exactly like ACTExactWinnerRemoval
 with TransferValueWithDenominatorWithNumOfBallots // Section 273 (9)(a)
 with SenateSurplusDistribution // Section 273 (9)(b)
 with SenateNewWinnersDuringSurplusesDistribution
 with SenateNewWinnersDuringExclusion
 with UnfairExclusionTieResolutuim // TODO
 {  
  
  //def declareNewWinnersWhileExcluding(candidate: Candidate, exhaustedBallots: Set[ACTBallot], newtotals: Map[Candidate, Rational], totalsWithoutNewWinners: Map[Candidate, Rational], newElectionWithoutFractionInTotals: Election[ACTBallot]):  List[(Candidate,Rational)]
  
  //def declareNewWinnersWhileDistributingSurpluses(totals: Map[Candidate, Rational], election:Election[ACTBallot]):  List[(Candidate,Rational)] 
  

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//         Bulk Exclusion - Section 273 (13A)
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  
  // Section 273 (29)
  def computeNotionalVotes(candidate: Candidate, totals: Map[Candidate, Rational] ): Rational = {
    totals.clone().filter(p => p._2 < totals(candidate)).foldLeft(Rational(0,1))(_+_._2)
  }
  
  def computeShortfall(candidate: Candidate, totals: Map[Candidate, Rational], quota: Rational): Rational = {
    quota - totals(candidate) 
  }
  
  def returnLeadingShortfall(totals: Map[Candidate, Rational], quota: Rational): Rational = {
    quota - totals.valuesIterator.max
  }
  
  def computeVacancyShortfall(totals: Map[Candidate, Rational], numRemainingVacancies: Int,  quota: Rational): Rational = {
    val orderedTotals = totals.toList.sortBy(_._2).reverse  // it does not matter how tie of equal values is resolved here, because we care only about values, hence - simple sort
   // orderedTotals.take(numRemainingVacancies).foldLeft(Rational(0,1))(_+(quota-totals(_._2)))  
   var aggregate = Rational(0,1)
   for (candidate <- orderedTotals.take(numRemainingVacancies)) aggregate += (quota - totals(candidate._1))
   aggregate 
  }
  
 def returnCandidateA(totals: Map[Candidate, Rational], vacancyShortfall: Rational): Option[Candidate] ={
   
   val pickedTotals = totals.clone().filter(p => computeNotionalVotes(p._1,totals) >= vacancyShortfall)   // Section 273 (13A)(a)  
   if (pickedTotals.nonEmpty){
    pickedTotals.filter(p => p._2 == pickedTotals.valuesIterator.min) 
    if (pickedTotals.size != 1){ // not specified in the legislation how this case should be addressed.  Section 273 (13A)(a) says "stands lower or lowest in the poll"
      println("More than one candidate satisfy conditions of Candidate A: Section 273 (13A)(a). One of them is picked.") 
      Some(pickedTotals.head._1)
    }
    else Some(pickedTotals.head._1)
   }
   else None // CandidateA is unidentified
 }
  
 def returnCandidateB(totals: Map[Candidate, Rational], candidateA: Option[Candidate], vacancyShortfall: Rational): Option[Candidate] = {
   var totalsOfCandidatesPotentiallyB:  Map[Candidate, Rational] = Map()
   candidateA match {
    case Some(cA) => {
      totalsOfCandidatesPotentiallyB = totals.clone().filter(p => p._2 < totals(cA))
     // val candidateB = totals.clone().filter(p => p._2 == valueOfCandidateB).head._1
    }
    case None => {
      totalsOfCandidatesPotentiallyB = totals.clone().filter(p => computeNotionalVotes(p._1, totals) < vacancyShortfall)
      //val candidateB = totals.clone().filter(p => p._2 == valueOfCandidateB).head._1
    }
  }
  val orderedTotalsOfCandidatesPotentiallyB = totalsOfCandidatesPotentiallyB.toList.sortBy(_._2) //TODO: sort appropriately
  val candidatesB = for ((left,right) <- (orderedTotalsOfCandidatesPotentiallyB zip orderedTotalsOfCandidatesPotentiallyB.tail) if (computeNotionalVotes(left._1, totals) < right._2)) yield left
  if (candidatesB.nonEmpty) Some(candidatesB.head._1) else None  // TODO: tail?
 }
 
 
 def returnCandidateC(totals: Map[Candidate, Rational], leadingShortFall: Rational): Candidate = {
  val potentialCandidatesC = totals.clone().filter(p => computeNotionalVotes(p._1, totals)<leadingShortFall)
  potentialCandidatesC.toList.sortBy(_._2).head._1  //TODO: sort appropriately
 }
  
 
 def selectCandidatesForBulkExclusion(totals: Map[Candidate, Rational], numRemainingVacancies: Int, quota: Rational): List[(Candidate, Rational)] = {
   val orderedCandidates = totals.toList.sortBy(_._2) //TODO: sort appropriately
   println("orderedCandidates: " + orderedCandidates)
   val vacancyShortfall = computeVacancyShortfall(totals, numRemainingVacancies, quota)
   println("vacancyShortfall: " + vacancyShortfall)
   val candidateA = returnCandidateA(totals, vacancyShortfall)
   println("candidateA: " + candidateA)
   val candidateB = returnCandidateB(totals, candidateA, vacancyShortfall)
   println("candidateB: " + candidateB)
   candidateB match {
     case Some(cB) => {  // "in a case where Candidate B has been identified"
       val notionalVotesOfB = computeNotionalVotes(cB, totals)
       val leadingShortfall = returnLeadingShortfall(totals, quota)
       if (notionalVotesOfB < leadingShortfall){ // Section273 (13A)(c)
         orderedCandidates.take(orderedCandidates.indexOf(cB)+1)
       }
       else{ // Section273 (13A)(d)
        val candidateC = returnCandidateC(totals, leadingShortfall)
        orderedCandidates.take(orderedCandidates.indexOf(candidateC)+1)
       }
     }
     case None => List()  // Candidate B has not been identified
   }
 }
 
  
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  def filterBallotsWithFirstPreferences(election: Election[ACTBallot], preferences: List[Candidate]): Election[ACTBallot] = {
    var ballots:  Election[ACTBallot] = List()
    for (b <- election) {
      if (b.preferences.take(preferences.length) == preferences) ballots = b::ballots
    }
    ballots
  }
  

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Differs from ACTMethod as follows:
// - absence of ``Last Parcel'', hence None in place of markings 
// - existence of ``Bulk exclusion''
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  def computeWinners(election: Election[ACTBallot], ccandidates: List[Candidate], numVacancies: Int): List[(Candidate,Rational)] = {
    
   println(" \n NEW RECURSIVE CALL \n")
   
   //println("Election: " + election)
  
   if (election.isEmpty){Nil}  // If all ballots are removed by the candidate who reached the quota exactly, the election will be empty.
   //                             For example (3 seats, quota=2):
   //                              1 1/1 2
   //                              2 1/1 2
   //                              3 1/1 2
   //                              4 1/1 5>6>1
   //                              5 1/1 5>3>6 
   else {
   
   //val ccands = getCandidates(election)
   println("Continuing candidates: " + ccandidates)
       
   val totals = computeTotals(election, ccandidates)  
   println("Totals: " + totals)
        
   //result.addTotalsToHistory(totals)
    
   // TODO: Section 273(17) (when only two continuing candidates remain for a single seat)
   // Notice: There may be more new winners than available vacancies!!! 
   if (ccandidates.length == numVacancies){ // Section 273(18)
     val ws = for (c <- ccandidates) yield (c, totals.getOrElse(c, Rational(0,1)))
     report.newCount(VictoryWithoutQuota, None, None, None, Some(ws), None)
     report.setLossByFractionToZero
     for (c <- ccandidates) yield (c, totals.getOrElse(c, Rational(0,1)))
   }
   else {        
    quotaReached(totals, result.getQuota) match {
      case true => {
          val winners: List[(Candidate, Rational)] = returnNewWinners(totals, result.getQuota) // sorted! tie resolved!
          println("New winners: " + winners)
          result.addPendingWinners(winners.toList, None) 
      
          vacanciesFilled(winners.length, numVacancies) match {
              case false =>  {
                println("Vacancies: not yet filled.")
                val res = surplusesDistribution(election, ccandidates, numVacancies-winners.length)
                val newElection: Election[ACTBallot] = res._1
                val newWinners: List[(Candidate, Rational)] = res._2
                
                val nws = winners.length + newWinners.length
                println("Number of winners in this recursive call: "  + nws)
                val allWinners = winners:::newWinners 
                if (nws == numVacancies) { allWinners } 
                else {
                  computeWinners(newElection, ccandidates.filterNot(allWinners.map{_._1}.toSet.contains(_)) ,numVacancies-nws):::allWinners  // TODO: care should be taken that newElection is not empty?!
                }
                }
              case true => winners
            }
      }    
      case false =>  {
        // Section 273 (13)(b) => (13A) and (13)(a) => (13AA) 
        
        var candidatesToExclude:  List[(Candidate, Rational)] = List()
        
        val candidatesForBulkExclusion = selectCandidatesForBulkExclusion(totals, numVacancies, result.getQuota)
        if (candidatesForBulkExclusion.nonEmpty) {  // DO BULK EXCLUSION  -  Section 273 (13)(b) => (13A)
         candidatesToExclude = candidatesForBulkExclusion  
        }
        else {  // Exclude the least voted candidate  - Section 273 (13)(a) => (13AA) 
            val leastVotedCandidate = chooseCandidateForExclusion(totals)
            println("Candidate to be excluded: " + leastVotedCandidate )
            result.addExcludedCandidate(leastVotedCandidate._1,leastVotedCandidate._2)
            candidatesToExclude = leastVotedCandidate::candidatesToExclude
        }
         val res = exclusion(election, ccandidates, candidatesToExclude, numVacancies)
         val newElection: Election[ACTBallot]  = res._1
         val newWinners: List[(Candidate, Rational)] = res._2
         println("New winners: " + newWinners)
         println("Number of winners in this recursive call: "  + newWinners.length)
         if (newWinners.length == numVacancies) { 
            // Notice: There may be more new winners than available vacancies!!! 
            // if (for_each_candidate(candidates, &check_status,(void *)(CAND_ELECTED|CAND_PENDING)) == num_seats) return true;
            newWinners }           
         else computeWinners(newElection,ccandidates.filterNot(x => candidatesToExclude.map(_._1).contains(x)), numVacancies-newWinners.length):::newWinners 
      }
      }
    
   }
   }
  }
 
 //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// like ACT, but no fraction loss
 def tryToDistributeSurplusVotes(election: Election[ACTBallot], ccandidates: List[Candidate], winner: Candidate, ctotal:Rational, markings: Option[Set[Int]] ): (Election[ACTBallot], List[(Candidate,Rational)]) = {

  val pendingWinners = result.getPendingWinners.map(x => x._1)

  if (ctotal == result.getQuota) { 
      val newElection = removeWinnerWithoutSurplusFromElection(election, winner)
      result.removePendingWinner(winner)
      (newElection, List())
   }
  else   
    // NOTE THAT WHEN (!ballotsAreContinuing(winner, election, pendingWinners))  THE ELECTION DOES NOT CHANGE
    //
    //  if (!ballotsAreContinuing(winner, election, pendingWinners) ) {
    //    val newElection = ???
    //    result.removePendingWinner(winner)
    //    (newElection, List())
    //  }
    //  else 
    {
    println("Distributing the surplus of " + winner) 
    
    val surplus = ctotal - result.getQuota
    
    val tv = computeTransferValue(surplus, election, pendingWinners, winner, None) 
    println("tv = " + tv)
        
    val (newElection, exhaustedBallots, ignoredBallots) = distributeSurplusVotes(election, winner, ctotal, None, pendingWinners, tv)  
           
    val newtotals = computeTotals(newElection, ccandidates)
    val newtotalsWithoutPendingWinners = newtotals.clone().retain((k,v) => !pendingWinners.contains(k)) 
    
    
    result.removePendingWinner(winner)
    
    result.addTotalsToHistory(newtotalsWithoutPendingWinners)
    var ws = declareNewWinnersWhileDistributingSurpluses(newtotalsWithoutPendingWinners,newElection)

    //------------ Reporting ------------------------------------------
    if (ws.nonEmpty) report.newCount(SurplusDistribution, Some(winner), Some(newElection), Some(newtotals), Some(ws), Some(exhaustedBallots))
    else report.newCount(SurplusDistribution, Some(winner), Some(newElection), Some(newtotals), None, Some(exhaustedBallots))
    ignoredBallots match { // ballots ignored because they don't belong to the last parcel of the winner
      case Some(ib) => report.setIgnoredBallots(ib)
      case None =>
    }
    //------------------------------------------------------------------

    (newElection, ws)
  }
 }
 
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// similar to ACT's exclusion. 
// much cleaner because there is not fraction loss
// but it implements a bulk exclusion
 def exclusion(election: Election[ACTBallot], ccandidates: List[Candidate], candidatesForExclusion: List[(Candidate, Rational)], numVacancies: Int): (Election[ACTBallot],  List[(Candidate, Rational)] ) = { 
   println("Vacancies left: " + numVacancies)
   

  var ws: List[(Candidate,Rational)] = List()
  var newws: List[(Candidate,Rational)] = List()
  var newElection = election
  var exhaustedBallots: Set[ACTBallot] = Set()
   
  for (candidate <- candidatesForExclusion) {
   
    if (candidate._2 == Rational(0,1)){ 
      println("Excluding candidate with zero votes: " + candidate)
      val ex = excludeZero(election, candidate._1)
      newElection = ex._1
    }
    else {
       var steps = determineStepsOfExclusion(election,candidate._1)

       while (ws.length != numVacancies && !steps.isEmpty){
         val step = steps.head
         println("Step of exclusion: " + step)
         steps = steps.tail // any better way to do this?
       
         val ex = exclude(newElection, step._1, Some(step._2), Some(newws.map(x => x._1)))

         newElection = ex._1
         exhaustedBallots = ex._2

         val totals = computeTotals(newElection, ccandidates)
         val totalsWithoutNewWinners = totals.clone().retain((k,v) => !ws.map(_._1).contains(k)) // excluding winners that are already identified in the while-loop
    
         result.addTotalsToHistory(totals)
         println("Totals: " + totals)
    
         newws = declareNewWinnersWhileExcluding(candidate._1, exhaustedBallots, totals, totalsWithoutNewWinners, newElection)
    
         ws = ws ::: newws 
       }
    }
  }
   
  var dws:  List[(Candidate, Rational)]  = List()
     if (ws.nonEmpty) {
       val res = surplusesDistribution(newElection, ccandidates.filterNot { x => candidatesForExclusion.map(_._1).contains(x) }, numVacancies - ws.length)
       newElection = res._1
       dws = res._2
     }
  (newElection, ws:::dws)
 }
  
 

  
  
  
}