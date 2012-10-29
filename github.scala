import org.eclipse.egit.github.core
import core.Authorization
import core.service._, core.client._
import scala.tools.jline.console.ConsoleReader
import scala.collection.JavaConversions._
import scalax.file.Path

object GitHub {
  lazy val client: GitHubClient = {
    val c = new GitHubClient().setOAuth2Token(validOAuthToken)
    println("authenticated user : " + new UserService(c).getUser.getLogin)
    c
  }

  def validOAuthToken: String = {
    val requiredScopes = List("repo")

    loadOrCreateAndStore(Path.fromString(System.getProperty("user.home")) / ".github-oath", { token =>
      val c = new GitHubClient().setOAuth2Token(token)
      new UserService(c).getUser
      true
    }, {
      val authorization = new Authorization().setScopes(requiredScopes).setNote("Scala script")
      new OAuthService(createGitHubClientWithCredentialsFromCommandLine).createAuthorization(authorization).getToken
    })
  }

  def createGitHubClientWithCredentialsFromCommandLine: GitHubClient = {
    println("Enter your GitHub username and password...")

    val cr = new ConsoleReader
    val username = cr.readLine("username>")
    val password = cr.readLine("password>", '*')
    new GitHubClient().setCredentials(username, password)
  }

  lazy val userService = new UserService(client)
  lazy val organizationService = new OrganizationService(client)
  lazy val repositoryService = new RepositoryService(client)


  def loadOrCreateAndStore(store: Path, candidateChecker: String => Boolean, generator: => String) : String = {
    implicit val codec = scalax.io.Codec.UTF8

    println("Checking for stored credentials in "+store.toAbsolute)
    val fileContents = try { Some(store.string) } catch { case _ => None }
    val validStoredToken = fileContents.filter(s => try { candidateChecker(s) } catch { case _ => false })

    validStoredToken match {
      case Some(token) => token
        println("(using stored credentials)")
        token
      case None =>
        val token: String = generator
        store.write(token)
        token
    }
  }
}
