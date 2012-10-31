import org.eclipse.egit.github.core
import core.{RepositoryHook, Repository}
import scala.collection.immutable.{SortedMap, TreeMap}
import scala.collection.JavaConversions._
import tools.jline.console.ConsoleReader

object OrgHookReport extends App {

  val repositories = GitHub.repositoryService.getOrgRepositories("guardian")

  println
  repositories.par.map(detailsForRepo).toList.sortBy(_.name.toLowerCase).foreach {
    rd =>
    println("%s (%d hooks)".format(rd.name,rd.hooks.size))
    rd.hooks.foreach {
      hook =>
        println("\t%s : %s %s".format(hook.getName, hook.getUrl, sanitisedConfigFor(hook)))
    }
  }

  println("Enter the Pivotal Tracker token for the 'github' user...")

  val cr = new ConsoleReader
  val token = cr.readLine("token>")

  val hook: RepositoryHook = new RepositoryHook()
  hook.setActive(true)
  hook.setName("pivotaltracker")
  hook.setConfig(Map("token" -> token))

  repositories.par.foreach { r=>  GitHub.repositoryService.createHook(r, hook) }


  def sanitisedConfigFor(hook: RepositoryHook): SortedMap[String, String] = {
    val config = hook.getConfig.map {
      case (k, v) if Set("token", "password").contains(k) => (k, v.take(3) + "...")
      case c => c
    }
    TreeMap(config.toList: _*)
  }

  def detailsForRepo(r: Repository): RepoDetails = {
    RepoDetails(r.getName, GitHub.repositoryService.getHooks(r).toList.sortBy(_.getName))
  }
}

case class RepoDetails(name: String, hooks: List[RepositoryHook])
