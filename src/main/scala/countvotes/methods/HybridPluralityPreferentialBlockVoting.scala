package countvotes.methods

import countvotes.structures._

import scala.collection.mutable.{HashMap => MMap}


/**
  * https://en.wikipedia.org/wiki/Preferential_block_voting
  */

object HybridPluralityPreferentialBlockVoting extends Scrutiny[WeightedBallot] {

  def totalsForFirstNVacancies(election: Election[WeightedBallot],ccandidates: List[Candidate], numVacancies: Int): MMap[Candidate,Rational] = {
    //calculates the totals for first n candidates where n is equal to number of vacancies
    var m = new MMap[Candidate, Rational]
    for(b<-election if !b.preferences.isEmpty){
      for(c<-b.preferences.take(numVacancies)){
        m(c) = m.getOrElse(c, Rational(0,1)) + b.weight
      }
    }
    m
  }

  def exclude(election: Election[WeightedBallot],ccandidate: Candidate) : Election[WeightedBallot] ={
    var list: Election[WeightedBallot] = Nil
    for(b<-election if !b.preferences.isEmpty){
      list = WeightedBallot(b.preferences.filter(_!=ccandidate), b.id, b.weight) :: list
    }
    list
  }

  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  override def winners(election: Election[WeightedBallot], ccandidates: List[Candidate], numVacancies: Int): List[(Candidate, Rational)] = {
    var winnerlist: List[(Candidate, Rational)] = Nil
    var election1 = election
    var ccandidates1 = ccandidates
    var vacancies = numVacancies
    while(vacancies != 0)
    {
      var tls = totalsForFirstNVacancies(election1, ccandidates1, vacancies)
      val sortedCandList = tls.toList.sortWith(_._2 > _._2)
      if(sortedCandList.head._2 > (election1.size / 2)) {
        winnerlist = sortedCandList.head :: winnerlist
        election1 = exclude(election1,sortedCandList.head._1)
        ccandidates1 = ccandidates1.filter(_!=sortedCandList.head._1)
        vacancies = vacancies - 1
      } else {
        election1 = exclude(election1,tls.filter(x => ccandidates1.contains(x._1)).toList.sortWith(_._2<_._2).head._1)
        ccandidates1 = ccandidates1.filter(_!=sortedCandList.last._1)
      }
    }
    winnerlist
  }
}


