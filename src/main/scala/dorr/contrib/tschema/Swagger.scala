package dorr.contrib.tschema

import ru.tinkoff.tschema.swagger.{OpenApiHeader, OpenApiResponse, SwaggerPrimitive, SwaggerStringValue}

object Swagger {
  val uriSchema = new SwaggerPrimitive(SwaggerStringValue(), None)

  val MovedPermanently: OpenApiResponse = OpenApiResponse(
    description = Some("Moved Permanently"),
    headers = Map("Location" -> OpenApiHeader(
      description = Some("Target URI"),
      schema = Some(uriSchema)
    ))
  )
}
