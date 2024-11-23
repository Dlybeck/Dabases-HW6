import java.util.ArrayList;
import java.util.Scanner;

public class ExtHash {
    /**
     * Class representing a bucket of keys in an extendable hashmap
     */
    private static class Bucket {
        private final int bucketSize;
        private final String[] elems;
        private int currentSize;
        private final String address;
        private final int localDepth;  // Added local depth

        /**
         * Bucket constructor
         * @param bucketSize int representing how many keys can be stored in the bucket
         * @param address String representing the first x values of the keys it holds
         * @param localDepth int representing how many digits deep the hashmap is looking
         */
        Bucket(int bucketSize, String address, int localDepth) {
            this.bucketSize = bucketSize;
            this.elems = new String[bucketSize];
            this.currentSize = 0;
            this.address = address;
            this.localDepth = localDepth;
        }

        /**
         * Adds a new key to the bucket
         * @param key String representing the key being added
         * @return Boolean: true if it was successfully added, false if not
         */
        public boolean addToBucket(String key) {
            //Dont add if key is too big
            if (currentSize >= bucketSize) {
                return false;
            }
            // Only add if key matches bucket's address pattern
            if (key.startsWith(address)) {
                elems[currentSize] = key;
                currentSize++;
                return true;
            }
            //Cound not add
            return false;
        }

        /**
         * toString method for the Bucket object
         * @return String representing the bucket object
         */
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Local(").append(localDepth).append(")[");
            result.append(address).append("*] = [");

            boolean first = true;
            for (String elem : elems) {
                if (!first) result.append(", ");
                result.append(elem != null ? elem : "null");
                first = false;
            }
            result.append("]");
            return result.toString();
        }
    }

    // ExtHash fields
    int bitDepth;
    int bucketSize;
    int keyLength;
    ArrayList<Bucket> directory;

    /**
     * Constructor for the Extendable hashmap
     * @param bucketSize int representing how many keys the buckets can hold
     * @param keyLength int representing how long each key has to be
     */
    public ExtHash(int bucketSize, int keyLength) {
        this.bitDepth = 0;
        this.bucketSize = bucketSize;
        this.keyLength = keyLength;
        this.directory = new ArrayList<>();
        Bucket initialBucket = new Bucket(bucketSize, "", 0);
        this.directory.add(initialBucket);
    }

    /**
     * toString method for the ExtHash class
     * @return String representing the ExtHash object
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Global(").append(this.bitDepth).append(")\n");
        for (int i = 0; i < directory.size(); i++) {
            if (bitDepth > 0) {
                //Show binary index if global depth > 0
                String binIndex = Integer.toBinaryString(i);
                while (binIndex.length() < bitDepth) {
                    binIndex = "0" + binIndex;
                }
                result.append(binIndex);
            }
            result.append(": ").append(directory.get(i)).append("\n");
        }
        //Remove final new line and return full string
        return result.toString().trim();
    }

    /**
     * Finds the index of the Bucket that would hold the given key
     * @param data int representing the key to find the index for
     * @return int representing the index
     */
    private int getDirectoryIndex(String data) {
        if (this.bitDepth == 0) {
            return 0; // Default to the first bucket
        }
        String addressPrefix = data.substring(0, Math.min(data.length(), this.bitDepth));
        return Integer.parseInt(addressPrefix, 2); // Convert binary prefix to integer
    }

    public boolean insert(String data) {
        if(this.search(data)) return false;

        int index = getDirectoryIndex(data);
        Bucket bucket = this.directory.get(index);

        // Attempt to insert the data recursively
        if (!bucket.addToBucket(data)) {
            splitBucket(bucket); //Split the bucket
            insert(data);        //Retry insertion
        }
        return true;
    }

    /**
     * If the bucket is full it splits the bucket into more and rehashes
     * @param bucket Bucket that needs to be split
     */
    private void splitBucket(Bucket bucket) {
        //Split the current bucket
        if (bucket.localDepth == this.bitDepth) {
            ArrayList<Bucket> oldDirectory = new ArrayList<>(directory);
            directory.clear();
            for (Bucket b : oldDirectory) {
                directory.add(b);
                directory.add(b);
            }
            this.bitDepth++;
        }

        String addr0 = bucket.address + "0";
        String addr1 = bucket.address + "1";
        int newLocalDepth = bucket.localDepth + 1;

        //Assign the new buckets with adjusted addresses
        Bucket bucket0 = new Bucket(this.bucketSize, addr0, newLocalDepth);
        Bucket bucket1 = new Bucket(this.bucketSize, addr1, newLocalDepth);

        //Redistribute existing keys
        String[] oldKeys = bucket.elems.clone();
        for (String key : oldKeys) {
            if (key != null) {
                if (key.startsWith(addr0)) {
                    bucket0.addToBucket(key);
                } else if (key.startsWith(addr1)) {
                    bucket1.addToBucket(key);
                }
            }
        }

        //Update directory entries
        for (int i = 0; i < directory.size(); i++) {
            if (directory.get(i) == bucket) {
                String binIndex = String.format("%" + bitDepth + "s",
                        Integer.toBinaryString(i)).replace(' ', '0');
                if (binIndex.startsWith(addr0)) {
                    directory.set(i, bucket0);
                } else {
                    directory.set(i, bucket1);
                }
            }
        }
    }

    /**
     * Searches to see if a given key is in teh extendable hashmap
     * @param key String the key to search for
     * @return Boolean true if there false if not
     */
    public boolean search(String key) {
        // Get the directory index for the given key
        int index = getDirectoryIndex(key);
        Bucket bucket = this.directory.get(index);

        // Check if the key exists in the bucket
        for (String str : bucket.elems) {
            if (str != null && str.equals(key)) {
                return true; // Key found
            }
        }
        return false; // Key not found
    }

    /**
     * Process the input given by the user alerting of any errors
     * @param input String[] of the inputs given by the user
     * @return Boolean true if ready for another input false if not
     */
    public boolean processInput(String[] input) {
        if (input.length != 2) {
            //Quit
            if (input.length == 1 && input[0].equals("q")) return false;
            //Print
            if (input.length == 1 && input[0].equals("p")) {
                System.out.println(this);
                return true;
            }
            System.err.println("Error: Please provide valid input (e.g., 'i <key>' or 's <key>')");
            return true;
        }

        String command = input[0];
        String key = input[1];

        // Validate key length
        if (key.length() > this.keyLength) {
            System.err.println("Error: Key exceeds maximum length of " + this.keyLength);
            return true;
        } else if (key.length() < this.keyLength) {
            System.err.println("Error: Key must be exactly " + this.keyLength + " characters long");
            return true;
        }

        switch (command) {
            case "i": // Insert
                if(this.insert(key)) System.out.println("SUCCESS");
                else System.out.println("FAILED");
                break;
            case "s": // Search
                if (this.search(key)) System.out.println(key + " FOUND");
                else System.out.println(key + " NOT FOUND");
                break;
            default:
                System.err.println("Error: Unknown command. Use 'i', 's', 'p', or 'q'.");
        }
        return true;
    }


    /**
     * Main method
     * @param args String[] program arguments
     */
    public static void main(String[] args) {
        //Read in the arguments
        int[] programInput = processArgs(args);
        ExtHash myHash = new ExtHash(programInput[0], programInput[1]);

        Scanner scanner = new Scanner(System.in);
        boolean run = true;

        //keep asking the user until 'q' is submitted
        while (run) {
            System.out.print("\n> ");
            String usrInput = scanner.nextLine();
            run = myHash.processInput(usrInput.split(" "));
        }
    }

    /**
     * Process the arguments when the program is started
     * @param args String[] program arguments
     * @return int[] index 0 is the bucket size, index 1 is the key size
     */
    private static int[] processArgs(String[] args){
        int[] input = new int[2];
        if(args.length == 2){
            for(int i = 0; i < args.length; i++){
                try{
                    input[i] = Integer.parseInt(args[i]);
                    if(input[i] <= 0) throw new RuntimeException();
                }
                catch(Exception e){
                    System.err.println("Make sure both arguments are positive integers");
                    System.exit(1);
                }
            }
        }
        else{
            System.err.println("Usage: java ExtHash <block size> <key length>");
            System.exit(1);
        }
        return input;
    }
}
