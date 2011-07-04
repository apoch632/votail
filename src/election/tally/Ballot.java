/**
 * Votail - PR-STV ballot counting for Irish elections
 * 
 * Copyright (c) 2005-2009 Dermot Cochran, Joseph R. Kiniry
 * and Patrick E. Tierney
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package election.tally;

import java.io.Serializable;

// @ refine "Ballot.jml";

/*
 * <BON>
 * class_chart BALLOT
 * indexing
 * author: "Dermot Cochran"
 * explanation
 * "An electronic representation of a valid ballot paper"
 * query
 * "To which continuing candidate is this ballot allocated?",
 * "In which round of counting was this ballot last transfered?",
 * "Who is the first preference candidate on this ballot?",
 * "Is this ballot deeper in the pile than another ballot?",
 * "What is the internal identifier (sequence number) for this ballot?"
 * command
 * "Load this ballot from the database"
 * constraint
 * "Every valid ballot has a valid first preference"
 * </BON>
 */

/**
 * The ordered set of preferences from a ballot paper in an Irish election,
 * which uses the Proportional Representation Single Transfer Vote. (PRSTV)
 * system.
 * 
 * @see <a href="http://www.cev.ie/htm/tenders/pdf/1_2.pdf"> Department of
 *      Environment and Local Government, Count Requirements and Commentary on
 *      Count Rules, section 3-14</a>
 * @author <a href="http://kind.ucd.ie/documents/research/lgsse/evoting.html">
 *         Dermot Cochran</a>
 */

public final class Ballot implements Serializable {
  
  private static final long serialVersionUID = -2377214384195511416L;

  protected static final char WHITE_SPACE = ' ';
  
  /**
   * Maximum possible number of ballots based on maximum population size for a
   * five seat constituency i.e. at most 30,000 people per elected
   * representative.
   * 
   * @see "Constitution of Ireland, Article 16, Section 2"
   */
  public static final int MAX_BALLOTS = 150000;
  
  /**
   * Candidate ID value to use for non-transferable ballot papers
   * 
   * @design A special candidate ID value is used to indicate non-transferable
   *         votes i.e., when the list of preferences has been exhausted and
   *         none of the continuing candidates are in the preference list, the
   *         ballot is deemed to be non-transferable.
   * @see <a href="http://www.cev.ie/htm/tenders/pdf/1_2.pdf"> Department Of
   *      Environment and Local Government, Count requirements and Commentary an
   *      Count Rules, section 7, pages 23-27</a>
   */
  public static final int NONTRANSFERABLE = 0;
  
  
  /** Indicates that a candidate has not received any preference in the ballot */
  public static final int NOT_APPROVED = Integer.MAX_VALUE;
  
  /** List of candidates in order of preference */
  protected/*@ spec_public non_null */int[] preferenceList;
  
  /** Total number of valid preferences on this ballot paper */
  //@ invariant 0 <= numberOfPreferences;
  //@ invariant numberOfPreferences <= preferenceList.length;
  protected /*@ spec_public @*/ int numberOfPreferences;
  
  /** Position within preference list */
  //@ initially positionInList == 0;
  //@ constraint \old(positionInList) <= positionInList;
  //@ invariant 0 <= positionInList;
  //@ invariant positionInList <= numberOfPreferences;
  protected transient /*@ spec_public @*/int positionInList;
  
  /**
   * Generate an empty ballot paper for use by a voter.
   */
  /*@ public normal_behavior
    @   requires (\forall int i; 0 <= i && i < preferences.length;
    @     preferences[i] != NONTRANSFERABLE &&
    @     preferences[i] != Candidate.NO_CANDIDATE);
    @   assignable numberOfPreferences, positionInList, preferenceList[*], preferenceList;
    @   ensures (\forall int index; 0 <= index && index < numberOfPreferences;
    @     preferenceList[index] == preferences[index]);
    @*/
  public Ballot(final/*@ non_null @*/int[] preferences) {
    numberOfPreferences = preferences.length;
    positionInList = 0;
    preferenceList = new int[numberOfPreferences];
    for (int index = 0; index < numberOfPreferences; index++) {
      preferenceList[index] = preferences[index];
    }
  }
  
  /**
   * Get candidate ID to which the ballot is assigned
   * 
   * @return The candidate ID to which the ballot is assigned
   */
  /*@ ensures \result == getPreference(positionInList);
    @*/
  public final/*@ pure @*/int getCandidateID() {
    return getPreference(positionInList);
  }
  
  /**
   * Get next preference candidate ID
   * 
   * @param offset
   *          The number of preferences to look ahead
   * @return The next preference candidate ID
   */
  /*@ public normal_behavior
    @   requires 0 <= positionInList + offset;
    @   ensures (\result == NONTRANSFERABLE)
    @     || (\result == getPreference(positionInList + offset));
    @*/
  public/*@ pure @*/int getNextPreference(final int offset) {
    final int index = positionInList + offset;
    if (index < numberOfPreferences && index < preferenceList.length) {
      return preferenceList[index];
    }
    return NONTRANSFERABLE;
  }
  
  /**
   * Transfer this ballot to the next preference candidate.
   * 
   * @design This method may be called multiple times during the same count
   *         until the ballot is non-transferable or a continuing candidate ID
   *         is found in the remainder of the preference list.
   * @param countNumber
   *          The count number at which the ballot was transfered.
   */
  /*@ public normal_behavior
    @   assignable positionInList;
    @   ensures (\old(positionInList) < numberOfPreferences) ==>
    @     \old(positionInList) <= positionInList;
    @   ensures (\old(positionInList) == numberOfPreferences) ==>
    @     positionInList == \old(positionInList);
    @*/
  public void transfer() {
    
    if (positionInList < numberOfPreferences) {
      positionInList++;
    }
  }
  
  /**
   * This method checks if this ballot paper is assigned to this candidate.
   * 
   * @design It is valid to use <code>NONTRANSFERABLE</code> as the ID value to
   *         be checked. This ballot paper can only be assigned to one candidate
   *         at a time; there are no fractional transfer of votes in Dail
   *         elections.
   * @param candidateIDToCheck
   *          The unique identifier for this candidate
   * @return <code>true</code> if this ballot paper is assigned to this
   *         candidate
   *         ID
   */
  /*@ public normal_behavior
    @   ensures (\result == true) <==> (getCandidateID() == candidateIDToCheck);
    @*/
  public/*@ pure @*/boolean isAssignedTo(final int candidateIDToCheck) {
    return (getCandidateID() == candidateIDToCheck);
  }
  
  /**
   * Gets the remaining number of preferences.
   * 
   * @return The number of preferences remaining
   */
  /*@ public normal_behavior 
    @     requires positionInList <= numberOfPreferences;
    @     ensures \result == numberOfPreferences - positionInList;
    @*/
  public/*@ pure @*/int remainingPreferences() {
    return (numberOfPreferences - positionInList);
  }
  
  /*@ requires 0 <= index;
    @ requires index <= numberOfPreferences;
    @ ensures (index < numberOfPreferences) 
    @   ==> preferenceList[index] == \result;
    @ ensures (numberOfPreferences == index) 
    @   ==> \result == Ballot.NONTRANSFERABLE;
    @*/
  protected final/*@ spec_public pure @*/int getPreference(final int index) {
    if (index < numberOfPreferences) {
      return preferenceList[index];
    }
    
    return Ballot.NONTRANSFERABLE;
  }
  
  /**
   * Query: Is this the first preference on the ballot?
   * 
   * @param candidateID
   *          The <code>candidateID</code> for the first preference
   * @return <code>true</code> if this is the first preference on the ballot
   */
  public/*@ pure @*/boolean isFirstPreference(final int candidateID) {
    if (preferenceList.length == 0) {
      
      return false;
    }
    return candidateID == preferenceList[0];
  }
  
  /**
   * Convert the ballot into a text string
   * 
   * @return The ballot as a string
   */
  public/*@ non_null pure @*/final String toString() {
    StringBuffer stringBuffer = new StringBuffer("(");
    for (int i = 0; i < numberOfPreferences; i++) {
      final int nextPreference = preferenceList[i];
      
      if (0 < i) {
        stringBuffer.append(Ballot.WHITE_SPACE);
      }
      stringBuffer.append("" + nextPreference);
    }
    stringBuffer.append(")");
    return stringBuffer.toString();
  }
  
  /**
   * Has the candidate received any level of preference?
   * 
   * <p> Some variants of PR-STV use approval based elimination, to avoid early
   * elimination of candidates with large numbers of second or subsequent
   * preferences.
   * </p>
   * 
   * @param candidateID The identifier of the candidate
   * @return <code>True</code> if any preference for this candidate
   */
  /*@ public normal_behavior
    @ ensures \result == (\exists int i; 0 <= i && i < numberOfPreferences;
    @   candidateID == preferenceList[i]);
    @*/
  public/*@ pure @*/boolean isApproved(int candidateID) {
    for (int i = 0; i < numberOfPreferences; i++) {
      if (candidateID == preferenceList[i]) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Get the ranking of the candidate in the list of preferences
   * 
   * @param candidateID The candidate to be checked
   * @return The ranking of the candidate or else <code>NOT_APPROVED</code>
   */
  /*@ public normal_behavior
    @ ensures isApproved(candidateID) ==>
    @   (0 <= \result && \result <= numberOfPreferences);
    @ ensures !isApproved(candidateID) ==>
    @   (NOT_APPROVED == \result);
    @*/
  public/*@ pure @*/int getRank(int candidateID) {
    for (int i = 0; i < numberOfPreferences; i++) {
      if (candidateID == preferenceList[i]) {
        return i;
      }
    }
    
    return NOT_APPROVED;
  }

  /**
   * Get the full list of preferences from this ballot.
   * 
   * @return the preferenceList
   */
  //@ ensures \result == preferenceList;
  public /*@ pure @*/ int[] getPreferenceList() {
    return preferenceList;
  }

  /**
   * Get the length of this ballot.
   * 
   * @return the numberOfPreferences
   */
  public /*@ pure @*/ int getNumberOfPreferences() {
    return numberOfPreferences;
  }
}
