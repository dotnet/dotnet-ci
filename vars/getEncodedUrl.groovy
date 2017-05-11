import hudson.Util

/**
  * Retrieves an encoded URL element for Mission Control/Helix
  * While Jenkins's Util.rawEncode(...) works, it does not use the
  * same format for escape sequences as Angular.  % vs. ~.  So this simply
  * uses the Jenkins one and changes the % to ~.
  * @return Encoded url element.
  */
def call(String inputUrl) {
    String output = Util.rawEncode(inputUrl)
    return output.replaceAll("%", "~")
}