import org.eclipse.egit.github.core
import core.{RepositoryHook, Repository}
import scala.collection.immutable.{SortedMap, TreeMap}
import scala.collection.JavaConversions._

object OrgHookReport extends App {

  val repositories = GitHub.repositoryService.getOrgRepositories("guardian")

  println
  repositories.par.map(detailsForRepo).toList.sortBy(_.name.toLowerCase).foreach {
    rd =>
    println("%s (%d hooks)".format(rd.name,rd.hooks.size))
    rd.hooks.foreach {
      hook =>
        println("\t%s : %s".format(hook.getName, sanitisedConfigFor(hook)))
    }
  }


  def sanitisedConfigFor(hook: RepositoryHook): SortedMap[String, String] = {
    val config = hook.getConfig.map {
      case (k, v) if Set("token", "password").contains(k) => (k, v.take(2) + "...")
      case c => c
    }
    TreeMap(config.toList: _*)
  }

  def detailsForRepo(r: Repository): RepoDetails = {
    RepoDetails(r.getName, GitHub.repositoryService.getHooks(r).toList.sortBy(_.getName))
  }
}

case class RepoDetails(name: String, hooks: List[RepositoryHook])
