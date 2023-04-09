package name.fraser.neil.plaintext

import java.io.File

fun main(args: Array<String>) {
    val dmp = diff_match_patch()

    lateinit var text1: String
    lateinit var text2: String
    println("Welcome to Diff-Match-Patch!")
    val regex = "[12]".toRegex()
    var response = getValidatedInput(
        regex,
        "Would you like to input text or filenames? 1 for text, 2 for filenames",
        "Please input 1 or 2"
    )
    if (response == "2") {
        while (true) {
            if (runCatching {
                    val pair = requestFromFiles()
                    text1 = pair.first
                    text2 = pair.second
                }.isFailure) continue else break
        }
    } else {
        print("Input text 1: ")
        text1 = readLine()!!
        print("Input text 2: ")
        text2 = readLine()!!
    }
    val diff = dmp.diff_main(text1, text2)
    val divider = "-".repeat(20)
    println("Full diff: $diff")
    println(divider)
    val diffNotEqual = diff.filterNot { it.operation.equals(diff_match_patch.Operation.EQUAL) }
    println("Non-equal: $diffNotEqual")
    println("Semantic cleanup reduces the number of edits by eliminating semantically trivial equalities. Lossless semantic cleanup looks for single edits surrounded on both sides by equalities which can be shifted sideways to align the edit to a word boundary. e.g: The c<ins>at c</ins>ame. -> The <ins>cat </ins>came.")
    response = getValidatedInput(
        "[012]".toRegex(),
        "Would you like to do a semantic cleanup? 0 for no, 1 for yes, 2 for lossless",
        "Please enter 0,1, or 2"
    )
    if (response != "0") {
        if (response == "1") dmp.diff_cleanupSemantic(diff)
        else dmp.diff_cleanupSemanticLossless(diff)
        println("Cleaned up: $diff")
    }
    response = getValidatedInput(
        "[0123]".toRegex(),
        "Would you like to save these results to a file? 0 for no, 1 for saving the full diff to a file, 2 for saving the non-equal diff, 3 for saving both",
        "Please enter 1,2,3, or 4"
    )
    when (response) {
        "1" -> {
            print("Please enter the filepath to save it to: ")
            val name = readLine()!!
            File(name).writeText(diff.toString())
        }
        "2" -> {

            print("Please enter the filepath to save it to: ")
            val name = readLine()!!
            File(name).writeText(diffNotEqual.toString())
        }
        "3" -> {

            print("Please enter the filepath to save the full diff to: ")
            var name = readLine()!!
            File(name).writeText(diff.toString())
            print("Please enter the filepath to save the non-equal diff to: ")
            name = readLine()!!
            File(name).writeText(diffNotEqual.toString())
        }
    }
    // Result: [(-1, "Hell"), (1, "G"), (0, "o"), (1, "odbye"), (0, " World.")]
//        dmp.diff_cleanupSemantic(diff)
//        diff.removeAll { it.operation.equals(diff_match_patch.Operation.EQUAL) }
    // Result: [(-1, "Hello"), (1, "Goodbye"), (0, " World.")]
}

private fun requestFromFiles(): Pair<String, String> {
    print("Input filepath 1: ")
    val file1 = readLine()!!
    print("Input filepath 2: ")
    val file2 = readLine()!!
    return Pair(File(file1).readText(), File(file2).readText())
}

/**
 * Request input from the user by first printing [firstMessageToDisplay] and then calling readLine()
 * Loops for input until the input matches the provided [regex], printing [messageToDisplayOnError] every time the user enters an invalid input until a valid input is entered
 * @param regex the regex to check the input against
 * @param firstMessageToDisplay message to be displayed before input is requested; The string ": " will be appended.
 * @param messageToDisplayOnError message to be displayed when user's input does not  match [regex]; The string ": " will be appended.
 * @return an input from the user which matches [predicate]
 * */
fun getValidatedInput(
    regex: Regex,
    firstMessageToDisplay: String,
    messageToDisplayOnError: String
): String? {
    kotlin.io.print("$firstMessageToDisplay: ")
    var input = readLine()
    while (input?.matches(regex)
            ?.not() == true
    ) /*doesn't match regex (written in a roundabout way to retain nullability)*/ {
        kotlin.io.print("$messageToDisplayOnError: ")
        input = readLine()
    }
    return input
}