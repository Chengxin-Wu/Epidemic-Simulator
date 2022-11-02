// epidemic.java
/** Program that will eventually develop into an epidemic simulator
 * author Chengxin Wu
 * version March 22, 2021
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.Collections;
import java.util.LinkedList;

/** Error reporting framework
 *  All error messages go to System.err (aka stderr, the standard error stream).
 *  Currently, this only supports fatal error reporting.
 *  Later it would be nice to have a way to report non-fatal errors.
 */
class Error {
    private static int warningCount = 0;

    /** Report a fatal error
     *  @param msg -- error message to be output
     *  This never returns, the program terminates reporting failure.
     */
    public static void fatal( String msg ) {
        System.err.println( "Epidemic: " + msg );
        System.exit( 1 );  // abnormal termination
    }

    /** Non-fatal warning
     *  @param msg -- the warning message
     *  keeps a running count of warnings
     */
    public static void warn( String msg ) {
        System.err.println( "Warning: " + msg );
        warningCount = warningCount + 1;
    }

    /** Error exit if any warnings
     */
    public static void exitIfWarnings( String msg ) {
        if (warningCount > 0) fatal( msg );
    }
}

class MyScanner{
    private Scanner sc;
    
    public MyScanner( File f) throws FileNotFoundException {
        sc = new Scanner(f);
    }

    // methods that we wish we could inhereit from Scanner
    public boolean hasNext() { return sc.hasNext(); }
    public boolean hasNext( String s ) { return sc.hasNext( s ); }
    public String next() { return sc.next(); }

    // patterns that matter here

    // delimiters are spaces, tabs, newlines and carriage returns
    private static final Pattern delimPat = Pattern.compile( "[ \t\n\r]*" );

    // if it's not a name, it begins with a non-letter
    private static final Pattern NotNamePat
	= Pattern.compile( "([^A-Za-z]*)|" );

    // names consist of a letter followed optionally by letters or digits
    private static final Pattern namePat
	= Pattern.compile( "([A-Za-z][0-9A-Za-z]*)|" );

    // if it's not an int, it begins with a non-digit, non-negative-sign
    private static final Pattern NotIntPat
	= Pattern.compile( "([^-0-9]*)|" );

    // ints consist of an optional sign followed by at least one digit
    private static final Pattern intPat = Pattern.compile(
	"((-[0-9]|)[0-9]*)"
    );

    // at least one digit, with an optional point before between or after them
    private static final Pattern floatPat = Pattern.compile(
     "-?(([0-9]+\\.[0-9]*)|(\\.[0-9]+)|([0-9]*))"
    );

    /** tool to defer computation of messages output by methods of MyScanner
     *  To pass a specific message, create a subclass of Message to do it
     *  In general, this will be used to create lambda expressions, so
     *  users will not need to even know the class name!
     */
    public interface Message {
	    String myString();
    }

    /** get the next nae from the scanner or complain if missing
     *  See namePat for the details of what makes a float.
     *  @param defalt  -- return value if there is no next item
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @return the next item or the defalt
     */
    public String getNextName( String defalt, Message errorMessage ) {
        // first skip the delimiter, accumulate anything that's not a name
	    String notName = sc.skip( delimPat ).skip( NotNamePat ).match().group();

	    // second accumulate the name
	    String name = sc.skip( namePat ).match().group();

	    if (!notName.isEmpty()) { // there's something else a name belonged
	        Error.warn(
		    errorMessage.myString() + ": name expected, skipping " + notName
	        );
	    }

	    if (name.isEmpty()) { // missing name
	        Error.warn( errorMessage.myString() );
	        return defalt;
	    } else { // there was a name
	        return name;
        }
    }

    /** get the next integer from the scanner or complain if missing
     *  See intPat for the details of what makes a float.
     *  @param defalt  -- return value if there is no next integer
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @return the next integer or the defalt
     */
    public int getNextInt( int defalt, Message errorMessage ) {
	    // first skip the delimiter, accumulate anything that's not an int
	    String notInt = sc.skip( delimPat ).skip( NotIntPat ).match().group();

	    // second accumulate the int, if any
	    String text = sc.skip( delimPat ).skip( intPat ).match().group();

	    if (!notInt.isEmpty()) { // there's something else where an int belonged
	        Error.warn(
		    errorMessage.myString() + ": int expected, skipping " + notInt
	        );
	    }

	    if (text.isEmpty()) { // missing name
	        Error.warn( errorMessage.myString() );
	        return defalt;
	    } else { // the name was present and it matches intPat
	        return Integer.parseInt( text );
	    }
    }

    /** get the next float from the scanner or complain if missing
     *  See floatPat for the details of what makes a float.
     *  @param defalt  -- return value if there is no next integer
     *  @param defalt  -- return value if there is no next float
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @return the next float or the defalt
     */
    public float getNextFloat( float defalt, Message errorMessage ) {
	    // skip the delimiter, if any, then the float, if any; get the latter
	    String text = sc.skip( delimPat ).skip( floatPat ).match().group();

	    if (text.isEmpty()) { // missing name
	        Error.warn( errorMessage.myString() );
	        return defalt;
	    } else { // the name was present and it matches intPat
	        return Float.parseFloat( text );
	    }
    }

    // patterns for use with the NextLiteral routines
    public static final Pattern semicolon = Pattern.compile( ";|" );

    /** try to get the next literal from the scanner
     *  @param literal -- the literal to get
     *  @returns true if the literal was present and skipped, false otherwise
     *  The literal parameter must be a pattern that can match the empty string
     *  if the desired literal is not present.
     */
    public boolean tryNextLiteral() {
	    sc.skip( delimPat ); // allow delimiter before literal!
	    String s = sc.skip( semicolon ).match().group();
	    return !s.isEmpty();
    }

    /** get the next literal from the scanner or complain if missing
     *  @param literal -- the literal to get
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @see tryNextLiteral for the mechanism used.
     */
    public void getNextLiteral(Message errorMessage ) {
	    if ( !tryNextLiteral() ) {
	     Error.warn( errorMessage.myString() );
	    }
    }
}

class Check {

    /** tool to defer computation of messages output by methods of Check
     *  To pass a specific message, create a subclass of Message to do it
     *  In general, this will be used to create lambda expressions, so
     *  users will not need to even know the class name!
     */
    public interface Message {
	    String myString();
    }

    /** Force a floating value to be positive
     *  @param value -- the value to check
     *  @param defalt -- the value to use if the check fails
     *  @param msg -- the error message to output if check fails
     *  @return either value if success or defalt if failure
     */
    public static double positive( double value, double defalt, Message msg ) {
	    if (value > 0.0) {
	        return value;
	    } else {
	        Error.warn( msg.myString() );
	        return defalt;
	}
    }

    /** Force a floating value to be non negative
     *  @param value -- the value to check
     *  @param defalt -- the value to use if the check fails
     *  @param msg -- the error message to output if check fails
     *  @return either value if success or defalt if failure
     */
    public static double nonNeg( double value, double defalt, Message msg ) {
	    if (value >= 0.0) {
	        return value;
	    } else {
	        Error.warn( msg.myString() );
	        return defalt;
	    }
    }
}

/** Wrapper extending class Random, turning it into a singleton class
 *  @see Random
 *  Ideally, no user should ever create an instance of Random, all use this!
 *  Users can call MyRandom.stream.anyMethodOfRandom() (or of MyRandom)
 *              or MyRandom.stream().anyMethodOfRandom()
 *  Users can allocate MyRandom myStream = MyRandom.stream;
 *                  or MyRandom myStream = MyRandom.stream();
 *  No matter how they do it, they get the same stream
 */
class MyRandom extends Random {
    /** the only random number stream
     */
    public static final MyRandom stream = new MyRandom(); // the only stream;

    // nobody can construct a MyRandom except the above line of code
    private MyRandom() {
	    super();
    }

    /* alternative access to the only random number stream
     * @return the only stream
     */
    public static MyRandom stream() {
	    return stream;
    }

    // add distributions that weren't built in

    /** exponential distribution
     *  @param mean -- the mean value of the distribution
     *  @return a positive exponentially distributed random value
     */
    public double nextExponential( double mean ) {
	    return mean * -Math.log( this.nextDouble() );
    }

    public double nextLogNormal( double median, double sigma ) {
	return Math.exp( sigma * this.nextGaussian() ) * median;
    }
}

class Population{

    // static variable
    public static int uninfected;
    public static int  infected;
    public static LinkedList<Person> allLatenPerson = new LinkedList<>();
    public static LinkedList<Person> allAsymptomaticPerson = new LinkedList<>();
    public static LinkedList<Person> allSymptomaticPerson = new LinkedList<>();
    public static LinkedList<Person> allBedriddenPerson = new LinkedList<>();
    public static LinkedList<Person> allRecoveredPerson = new LinkedList<>();
    public static LinkedList<Person> allDeadPerson = new LinkedList<>();
    public static LinkedList<Person> allPerson = new LinkedList<>();

    public Population(int pop, int inf){}
}

/** Places that people are associate with and may occupy.
 *  Every place is an instance of some kind of PlaceKind
 *  @see PlaceKind for most of the attributes of places
 */
class Place {
    // instance variables
    public final PlaceKind kind; // what kind of place is this?
    public int nums = 0;

    /** Construct a new place
     *  @param k -- the kind of place
     *  BUG:  Attributes such as disease transmissivity will be needed
     */
    public Place( PlaceKind k ) {
	kind = k;
    }
}

class PlaceKind{
    public String name;
    private double median; // median population for this category
    private double scatter;// scatter of size distribution for this
	public double trans; // transmissivity
    private double sigma;  // sigma of the log normal distribution
    public Place unfilledPlace = null; // a place of this kind being filled
    public int unfilledCapacity = 0;   // capacity of unfilledPlace
    public int unfilledCapacity2 = 0;

    private static LinkedList<PlaceKind> allPlaceKinds = new LinkedList<>();
    private static final MyRandom rand = MyRandom.stream();

    public PlaceKind( MyScanner in ) {

	name = in.getNextName( "???", ()->"place with no name" );
	median = in.getNextFloat(
	    9.9999F,
	    ()->"place " + name + ": not followed by median"
	);
	scatter = in.getNextFloat(
	    9.9999F,
	    ()->"place " + name + " " + median + ": not followed by scatter"
	);
	trans = in.getNextFloat(
		9.999F,
		()->"place " + name + " " + median + ": not followed by transmissivity");
	in.getNextLiteral(()->this.describe() + ": missing semicolon");

    if (findPlaceKind( name ) != null) {
	    Error.warn( this.describe() + ": duplicate name" );
	}
	// force the median to be positive
	median = Check.positive( median, 1.0F,
	    ()-> this.describe() + ": non-positive median?"
	);
	// force the scatter to be positive
	scatter = Check.nonNeg( scatter, 0.0F,()-> this.describe() + ": negative scatter?");
	// force the transmissivity to be positive
	trans = Check.nonNeg( trans, 0.0F, ()-> this.describe() + ": negative transmissivity?");
	sigma = Math.log( (scatter + median) / median );

	allPlaceKinds.add( this ); // include this in the list of all
    }

    /** Produce a full textual description of this place
     *  @return the description
     *  This shortens many error messages
     */
    private String describe() {
	return "place " + name + " " + median + " " + scatter;
    }

    /** Find or make a place of a particular kind
     *  @return the place
     *  This should be called when a person is to be linked to a place of some
     *  particular kind, potentially occupying a space in that place.
     */
    public static Place findPlace(PlaceKind pk) {
	if (pk.unfilledCapacity <= 0 ) { // need to make a new place
	    // make new place using a log-normal distribution for the size
	    pk.unfilledCapacity
		= (int)Math.round( rand.nextLogNormal( pk.median, pk.sigma) );
        pk.unfilledCapacity2 = pk.unfilledCapacity;
	    pk.unfilledPlace = new Place(pk);
	}
	pk.unfilledCapacity = pk.unfilledCapacity - 1;
	return pk.unfilledPlace;
    }

    public static PlaceKind findPlaceKind( String n ) {
	    for (PlaceKind pk: allPlaceKinds) {
	        if (pk.name.equals( n )) return pk;
	    }
	    return null; // category not found
    }
}

class State{
    // instance variables
    final String state;
    public double median;
    public double scatter;
    public double recover;
    public double sigma;

    // a list of all the state of illness
    public static final LinkedList<State> allState = new LinkedList<>();

    public State(String s, MyScanner in){
        state = s;
        median = in.getNextFloat(9.999f,
        ()->"state" + state + ": not follow by median" );
        scatter = in.getNextFloat(9.999f,
        ()->"state" + state + ": not follow by scatter" );
        recover = 0.0f;
        if (s.equals("Symptomatic") | s.equals("Bedridden")){
            recover = in.getNextFloat(9.999f, ()->"state" + state + ": not follow by possibility of recovery");
        }

        in.getNextLiteral(()->this.state + ": missing semicolon");

        // complain if the name is not unique
	    if (findState(state) != null) {
	        Error.warn( this.state + ": duplicate state of illness" );
	    }

        // force the median to be positive
	    median = Check.positive( median, 1.0F,()-> this.state + ": non-positive median?");
	    // force the scatter to be positive
	    scatter = Check.nonNeg( scatter, 0.0F,()-> this.state + ": negative scatter?");
	    sigma = (float)Math.log((scatter + median)/median);
        allState.add(this);
    }

    /** Find a category of place, by name
     *  @param n -- the name of the category
     *  @return the PlaceKind with that name, or null if none has been defined
     */
    public static State findState( String n ) {
	    for (State s: allState) {
	        if (s.state.equals(n)) return s;
	    }
	    return null; // category not found
    }
}


class Role{
    public String name;
    public PlaceKind pk;
    public double fraction;
    public double sum = 0;
    public int number = 0;
    
    public static LinkedList<Role> allRoles = new LinkedList<Role>();

    public Role(MyScanner in){
        name = in.getNextName( "???", ()->"place with no name" );
        fraction = in.getNextFloat(9.999f,
        ()->"role" + name + ": not follow by fraction" );
        boolean hasNext = in.hasNext(); // needed below for missing semicolon
	    while (hasNext && !in.tryNextLiteral()) {
            String placeName = in.getNextName( "???", ()->"role with no name" );
	        pk = PlaceKind.findPlaceKind( placeName );

            if (pk == null) {
		        Error.warn(
		        name + " " + placeName + ": undefined place?"
		        );
	        }
            hasNext = in.hasNext();
        }
        if (!hasNext) {
	        Error.warn(
		    name + ": missing semicolon?"
	        );
	    }
        // complain if the name is not unique
	    if (findRole( name ) != null) {
	        Error.warn( name + ": role name reused?" );
        }
        fraction = Check.positive( fraction, 0.0F,()-> name + ": negative population?");
        sum = sum + fraction;

	    allRoles.add( this ); // include this role in the list of all roles
    }

    public static Role findRole(String r){
        for (Role rl : allRoles){
            if (rl.name.equals(r)) return rl;
        }
        return null;
    }

    public static void populateRoles( int population, int infected ) {
	    int pop = population; // working copy used only in infection decisions
	    int inf = infected;   // working copy used only in infection decisions
	    final MyRandom rand = MyRandom.stream;
        if (allRoles.isEmpty()) Error.fatal( "no roles specified" );
        for (Role r: allRoles) {
	        // how many people are in this role
	        r.number = (int)Math.round( (r.fraction / r.sum) * population );

	        // make that many people and infect the right number at random
	        for (int i = 0; i < r.number; i++) {
		    Person p = new Person( r );

		    // the ratio inf/pop is probability this person is infected
		    if (rand.nextFloat() < ((float)inf / (float)pop)) {
		        p.state = "Laten";
                p.moveTime = Person.time(1);
                Population.allLatenPerson.add(p);
		        inf = inf - 1;
                p.role.pk.unfilledPlace.nums++;
		    }
		    pop = pop - 1;
	        }
	    }
    }
}

class Person{
    public Role role;
    public String state = "Uninfected";
    public float moveTime = 0;
    public Place pl;

    public static Random random = new Random();
    public static LinkedList<Person> allPerson = new LinkedList<>();

    public Person(Role r){
        role = r;
        pl = PlaceKind.findPlace(r.pk);
        pl.nums++;
        allPerson.add(this);
    }

    public static int time(int i){
        MyRandom rand = MyRandom.stream();
        double lognormal = Math.exp(State.allState.get(i).sigma * rand.nextGaussian()) * 
        State.allState.get(i).median;
        return (int)Math.round(lognormal);
    }

    public static void goThroughTimes(float days){
        for (Person p : allPerson){
            if (p.moveTime > days){
                updateState2(p);
            }
            if (p.moveTime == days || p.state.equals("Uninfected")){
                updateState(p,days);
            }
        }
        System.out.print(days + " ");
        System.out.print(Population.uninfected + " ");
        System.out.print(Population.allLatenPerson.size() + " ");
        System.out.print(Population.allAsymptomaticPerson.size() + " ");
        System.out.print(Population.allSymptomaticPerson.size() + " ");
        System.out.print(Population.allBedriddenPerson.size() + " ");
        System.out.print(Population.allRecoveredPerson.size() + " ");
        System.out.println(Population.allDeadPerson.size() + " ");
    }

    //Change the state
    public static void updateState(Person p, float days){
        if (p.state.equals("Uninfected") && days != 0.0f){
            double pro = Prot(p.role.pk, days);
            int rand1 = random.nextInt(100);
            if (rand1 < (pro * 100)){
                p.state = "Laten";
                Population.allLatenPerson.add(p);
                Population.uninfected--;
            }
        }
        if (p.state.equals("Laten")){
            p.state = "Asymptomatic";
            p.moveTime = p.moveTime + time(1);
            Population.allAsymptomaticPerson.add(p);
            Population.allLatenPerson.remove(p);
        } else if (p.state.equals("Asymptomatic")){
            p.state = "Symptomatic";
            p.moveTime = p.moveTime + time(2);
            Population.allSymptomaticPerson.add(p);
            Population.allAsymptomaticPerson.remove(p);
        } else if (p.state.equals("Symptomatic")){
            p.state = "Bedridden";
            p.moveTime = p.moveTime + time(3);
            Population.allBedriddenPerson.add(p);
            Population.allSymptomaticPerson.remove(p);
        } else if (p.state.equals("Bedridden")){
            p.state = "Dead";
            p.moveTime = 0;
            Population.allDeadPerson.add(p);
            Population.allBedriddenPerson.remove(p);
        }
    }

    public static void updateState2(Person p){
        if (p.state.equals("Bedridden")){
            int rand1 = random.nextInt(100);
            if (rand1 < State.allState.get(2).recover * 100){
                p.state = "Recovered";
                p.moveTime = 0;
                Population.allRecoveredPerson.add(p);
                Population.allBedriddenPerson.remove(p);
            } else {
                int rand2 = random.nextInt(99);
                if (rand2 == 50){
                    p.state = "Dead";
                    p.moveTime = 0;
                    Population.allDeadPerson.add(p);
                    Population.allBedriddenPerson.remove(p);
                } 
            }
        }
    }

    public static double Prot(PlaceKind pk, float time){
        //System.out.println(Math.pow(Math.E,(-(pk.unfilledPlace.nums * pk.trans * time))));
        /**System.out.println(pk.unfilledCapacity2 - pk.unfilledCapacity);
        return (1.0 - Math.pow(Math.E,(-((pk.unfilledCapacity2 - pk.unfilledCapacity) * pk.trans * time))));
        */
        double meanTime = 1.0/Population.infected * pk.trans;
        double infectedTime = MyRandom.stream.nextExponential(meanTime);
        return infectedTime;
    }

}

public class Epidemic{
    private static void buildModel(MyScanner in){
        int pop = 0;
        int infected = 0;
        float days = 0.0f;
        while (in.hasNext()){
            // each item begins with a keyword
	        String keyword = in.getNextName( "???", ()-> "keyword expected" );
            if (keyword.equals("infected")){
		        // get infected, semicolon
		        final int p = in.getNextInt( 1,
		        ()-> "population: missing integer");
		        in.getNextLiteral(
		        ()-> "population " + p + ": missing ;");

		        // sanity constraints on population
		        if (infected != 0) {
		            Error.warn( "population specified more than once" );
		        } else {
		            infected = p;
		        }
		        if (infected <= 0) {
		            Error.warn( "population " + p + ": not positive" );
		            infected = 1;
                }
                if (infected > pop){
                    Error.warn("Nums of infected should not larger than population");
                    infected = 1;
                }
            } else if (keyword.equals("latent")){
                new State("Laten", in);
            } else if (keyword.equals("asymptomatic")){
                new State("Asymptomatic", in);
            } else if (keyword.equals("symptomatic")){
                new State("Symptomatic", in);
            } else if (keyword.equals("bedridden")){
                new State("Bedridden", in);
            } else if (keyword.equals("end")){
                final float p = in.getNextFloat( 1.0F,
		        ()-> "population: missing integer");
		        in.getNextLiteral(
		        ()-> "population " + p + ": missing ;");

		        // sanity constraints on population
		        if (days != 0) {
		            Error.warn( "population specified more than once" );
		        } else {
		            days = p;
		        }
		        if (days <= 0) {
		            Error.warn( "population " + p + ": not positive" );
		            days = 1;
                }
            } else if (keyword.equals("population")){
                // get population, semicolon
		        final int p = in.getNextInt( 1,
		        ()-> "population: missing integer"
		        );
		        in.getNextLiteral(
		        ()-> "population " + p + ": missing ;"
		        );

		        // sanity constraints on population
		        if (pop != 0) {
		        Error.warn( "population specified more than once" );
		        } else {
		            pop = p;
		        }
		        if (pop <= 0) {
		        Error.warn( "population " + p + ": not positive" );
		        pop = 1;
		        }
	        } else if (keyword.equals("place")){
                new PlaceKind(in);
            } else if (keyword.equals("role")){
                new Role(in);
            }
        }
        Population.infected = infected;
        Population.uninfected = pop - infected;

        Role.populateRoles(pop, infected);

        for (float i = 0.0f; i < days; i = i + 1.0F){
            Person.goThroughTimes(i);
        }
    }

    /** The main method
     *  @param args -- the command line arguments
     *  Most of this code is entirely about command line argument processing.
     *  It calls buildModel and will eventuall also start the simulation.
     */
    public static void main( String[] args ) {
	    if (args.length < 1) Error.fatal( "missing file name" );
	    if (args.length > 1) Error.warn( "too many arguments: " + args[1] );
	    try {
	        buildModel( new MyScanner( new File( args[0] ) ) );
	        // BUG:  Simulate based on model just built?
	    } catch ( FileNotFoundException e ) {
	        Error.fatal( "could not open file: " + args[0] );
	    }
    }
}