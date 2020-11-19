package io.jenkins.plugins.gamekins

import hudson.model.User
import hudson.model.UserProperty
import io.jenkins.plugins.gamekins.challenge.Challenge
import io.jenkins.plugins.gamekins.challenge.DummyChallenge
import io.jenkins.plugins.gamekins.statistics.Statistics
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.StaplerRequest
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Property that is added to each [User] to extend his configuration and ability. Stores all current, completed and
 * rejected [Challenge]s, author names in git, score and participation information.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class GameUserProperty : UserProperty() {

    private val completedChallenges: HashMap<String, CopyOnWriteArrayList<Challenge>> = HashMap()
    private val currentChallenges: HashMap<String, CopyOnWriteArrayList<Challenge>> = HashMap()
    private var gitNames: CopyOnWriteArraySet<String>? = null
    private val participation: HashMap<String, String> = HashMap()
    private val pseudonym: UUID = UUID.randomUUID()
    private val rejectedChallenges: HashMap<String, CopyOnWriteArrayList<Pair<Challenge, String>>> = HashMap()
    private val score: HashMap<String, Int> = HashMap()

    /**
     * Adds an additional [score] to one project [projectName], since one user can participate in multiple projects.
     */
    fun addScore(projectName: String, score: Int) {
        this.score[projectName] = this.score[projectName]!! + score
    }

    /**
     * Sets a [challenge] of project [projectName] to complete and removes it from the [currentChallenges].
     */
    fun completeChallenge(projectName: String, challenge: Challenge) {
        var challenges: CopyOnWriteArrayList<Challenge>
        if (challenge !is DummyChallenge) {
            completedChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
            challenges = completedChallenges[projectName]!!
            challenges.add(challenge)
            completedChallenges[projectName] = challenges
        }

        challenges = currentChallenges[projectName]!!
        challenges.remove(challenge)
        currentChallenges[projectName] = challenges
    }

    /**
     * Computes the initial git names of the user by full, display and user name.
     */
    private fun computeInitialGitNames(): CopyOnWriteArraySet<String> {
        val set = CopyOnWriteArraySet<String>()
        if (user != null) {
            set.add(user.fullName)
            set.add(user.displayName)
            set.add(user.id)
        }
        return set
    }

    /**
     * Returns the list of completed Challenges by [projectName].
     */
    fun getCompletedChallenges(projectName: String?): CopyOnWriteArrayList<Challenge> {
        return completedChallenges[projectName]!!
    }

    /**
     * Returns the list of current Challenges by [projectName].
     */
    fun getCurrentChallenges(projectName: String?): CopyOnWriteArrayList<Challenge> {
        return currentChallenges[projectName]!!
    }

    /**
     * Returns the git author name sof the user.
     */
    fun getGitNames(): CopyOnWriteArraySet<String> {
        return gitNames ?: CopyOnWriteArraySet()
    }

    /**
     * Returns the [gitNames] as String with line breaks after each entry.
     */
    fun getNames(): String {
        if (user == null) return ""
        if (gitNames == null) gitNames = computeInitialGitNames()
        val builder = StringBuilder()
        for (name in gitNames!!) {
            builder.append(name).append("\n")
        }
        return builder.substring(0, builder.length - 1)
    }

    /**
     * Returns the [pseudonym] of the user for [Statistics].
     */
    fun getPseudonym(): String {
        return pseudonym.toString()
    }

    /**
     * Returns the list of rejected Challenges by [projectName].
     */
    fun getRejectedChallenges(projectName: String?): CopyOnWriteArrayList<Pair<Challenge, String>> {
        return rejectedChallenges[projectName]!!
    }

    /**
     * Returns the score of the user by [projectName].
     */
    fun getScore(projectName: String): Int {
        if (isParticipating(projectName) && score[projectName] == null) {
            score[projectName] = 0
        }
        return score[projectName]!!
    }

    /**
     * Returns the name of the team the user is participating in the project [projectName].
     */
    fun getTeamName(projectName: String): String {
        val name: String? = participation[projectName]
        //Should not happen since each call is of getTeamName() is surrounded with a call to isParticipating()
        return name ?: "null"
    }

    /**
     * Returns the parent/owner/user of the property.
     */
    fun getUser(): User {
        return user
    }

    /**
     * Checks whether the user is participating in the project [projectName].
     */
    fun isParticipating(projectName: String): Boolean {
        return participation.containsKey(projectName)
    }

    /**
     * Checks whether the user is participating in team [teamName] in the project [projectName].
     */
    fun isParticipating(projectName: String, teamName: String): Boolean {
        return if (participation[projectName] == null) false else participation[projectName] == teamName
    }

    /**
     * Adds a new [Challenge] to the user.
     */
    fun newChallenge(projectName: String, challenge: Challenge) {
        currentChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
        val challenges = currentChallenges[projectName]!!
        challenges.add(challenge)
        currentChallenges[projectName] = challenges
    }

    /**
     * Returns the XML representation of the user.
     */
    fun printToXML(projectName: String, indentation: String): String {
        val print = StringBuilder()
        print.append(indentation).append("<User id=\"").append(pseudonym).append("\" project=\"")
                .append(projectName).append("\" score=\"").append(getScore(projectName)).append("\">\n")

        print.append(indentation).append("    <CurrentChallenges count=\"")
                .append(getCurrentChallenges(projectName).size).append("\">\n")
        for (challenge in getCurrentChallenges(projectName)) {
            print.append(challenge.printToXML("", "$indentation        ")).append("\n")
        }
        print.append(indentation).append("    </CurrentChallenges>\n")

        print.append(indentation).append("    <CompletedChallenges count=\"")
                .append(getCompletedChallenges(projectName).size).append("\">\n")
        for (challenge in getCompletedChallenges(projectName)) {
            print.append(challenge.printToXML("", "$indentation        ")).append("\n")
        }
        print.append(indentation).append("    </CompletedChallenges>\n")

        print.append(indentation).append("    <RejectedChallenges count=\"")
                .append(getRejectedChallenges(projectName).size).append("\">\n")
        for (pair in rejectedChallenges[projectName]!!) {
            print.append(pair.first.printToXML(pair.second, "$indentation        ")).append("\n")
        }
        print.append(indentation).append("    </RejectedChallenges>\n")

        print.append(indentation).append("</User>")
        return print.toString()
    }

    /**
     * Updates the git names if the user configuration is saved.
     */
    override fun reconfigure(req: StaplerRequest, form: JSONObject?): UserProperty? {
        if (form != null) setNames(form.getString("names"))
        return this
    }

    /**
     * Rejects a given [challenge] of project [projectName] with a [reason].
     */
    fun rejectChallenge(projectName: String, challenge: Challenge, reason: String) {
        rejectedChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
        val challenges = rejectedChallenges[projectName]!!
        challenges.add(Pair(challenge, reason))
        rejectedChallenges[projectName] = challenges
        val currentChallenges = currentChallenges[projectName]!!
        currentChallenges.remove(challenge)
        this.currentChallenges[projectName] = currentChallenges
    }

    /**
     * Removes the participation of the user in project [projectName].
     */
    fun removeParticipation(projectName: String) {
        participation.remove(projectName)
    }

    /**
     * Removes all Challenges for a specific [projectName] and resets the score.
     */
    fun reset(projectName: String) {
        completedChallenges[projectName] = CopyOnWriteArrayList()
        currentChallenges[projectName] = CopyOnWriteArrayList()
        rejectedChallenges[projectName] = CopyOnWriteArrayList()
        score[projectName] = 0
    }

    /**
     * Sets the git names.
     */
    @DataBoundSetter
    fun setNames(names: String) {
        val split = names.split("\n".toRegex())
        gitNames = CopyOnWriteArraySet(split)
    }

    /**
     * Adds the participation of the user for project [projectName] and team [teamName].
     */
    fun setParticipating(projectName: String, teamName: String) {
        participation[projectName] = teamName
        score.putIfAbsent(projectName, 0)
        completedChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
        currentChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
        rejectedChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
    }

    /**
     * Sets the user of the property during start of Jenkins and computes the initial git namens.
     */
    override fun setUser(u: User) {
        user = u
        if (gitNames == null) gitNames = computeInitialGitNames()
    }
}
