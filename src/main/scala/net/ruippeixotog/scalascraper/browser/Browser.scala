package net.ruippeixotog.scalascraper.browser

import org.jsoup.Connection.Method._
import org.jsoup.nodes.Document
import org.jsoup.{Connection, Jsoup}

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable.{Map => MutableMap}

class Browser {
  val cookies = MutableMap.empty[String, String]

  def get(url: String) = execute(url, _.method(GET))
  def post(url: String, form: Map[String, String]) = execute(url, _.method(POST).data(form))

  private[this] def prepareConn(conn: Connection): Connection =
    conn.cookies(cookies).userAgent("jsoup/1.8.1")

  private[this] def execute(url: String, conn: Connection => Connection): Document =
    process(conn(prepareConn(Jsoup.connect(url))))

  private[this] def process(conn: Connection) = {
    val res = conn.execute()
    lazy val doc = res.parse

    cookies ++= res.cookies

    val redirectUrl =
      if(res.hasHeader("Location")) Some(res.header("Location"))
      else doc.select("head meta[http-equiv=refresh]").headOption.map { e =>
        e.attr("content").split(";").find(_.startsWith("url")).head.split("=")(1)
      }

    redirectUrl match {
      case None => doc
      case Some(url) => get(url)
    }
  }
}