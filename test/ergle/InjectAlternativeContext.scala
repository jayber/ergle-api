package ergle

import org.springframework.context.ApplicationContext
import org.specs2.mutable.Around
import org.specs2.execute.AsResult
import org.specs2.execute
import scala.reflect.runtime.{universe => ru}
import scala.reflect.api.JavaUniverse

/**
 * Aborted attempt to make a generic dependency injector for mocks during test
 * tried both java and scala reflection, scala TypeTags screwed me, as did it's effect on methods and fields using java.reflect
 */
class InjectAlternativeContext[A](ctx: ApplicationContext,
                               targetClass: Class[_],
                               methodName: String,
                               remedy: Object ) extends Around {

  def around[T](t: => T)(implicit evidence$1: AsResult[T]): execute.Result = {

    val victim = ctx.getBean(targetClass).asInstanceOf[A]

    targetClass.getDeclaredMethods().filter(_.getName.toLowerCase.contains("datastore")).foreach {
      method => println(s"method=${method.getName}; args="+method.getParameterTypes.foreach {
        ptype =>
          ptype.getName
      } +
      s"return type=${method.getReturnType.getName}")
    }

    targetClass.getMethod(methodName).invoke(victim, remedy)

    AsResult(t)
  }
}
