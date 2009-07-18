/**
 * 
 */
package ie.lero.evoting.scenario;

import junit.framework.TestCase;
import election.tally.BallotBox;
import election.tally.BallotCounting;
import election.tally.Candidate;
import election.tally.Election;
import election.tally.mock.MockBallot;


/**
 * @author Dermot Cochran
 *
 */
public class SelectHighestContinuingCandidateEventB extends TestCase {
  
    private static final int candidateToWin = 0;
    private BallotCounting ballotCounting;

    protected void setUp() throws Exception {
    super.setUp();
    BallotBox ballotBox = new BallotBox();
    ballotCounting = new BallotCounting();
    Election parameters = new Election();
    parameters.numberOfCandidates = 2;
    parameters.numberOfSeatsInThisElection = 1;
    parameters.totalNumberOfSeats = 3;
    Candidate[] candidateList = new Candidate[parameters.numberOfCandidates];
    candidateList[0] = new Candidate();
    candidateList[1] = new Candidate();
    parameters.setCandidateList(candidateList);
    ballotCounting.setup(parameters);
    MockBallot mockBallot = new MockBallot();
    mockBallot.setFirstPreference(candidateList[candidateToWin].getCandidateID());
    ballotBox.accept(mockBallot);
    ballotCounting.load(ballotBox);
  }

  /**
	 * Test selection of highest continuing candidate
	 */
	public void testEvent () {
	  ballotCounting.calculateFirstPreferences(); //@ nowarn;
		int winner = ballotCounting.findHighestCandidate();
		assertTrue (winner == candidateToWin);
	}

 	

 	

}
