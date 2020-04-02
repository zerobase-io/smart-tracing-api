## PDF Generation

The backend is generating PDFs for notifications (usually emailed). It's a multi-step process and depends on
some conventions.

### The Process
1. A Thymeleaf HTML template gets rendered.
1. The HTML is converted to XHTML for consistency using Tidy.
1. The XHTML is rendered as a PDF using the LGPL version of iText.

### The Conventions
* HTML templates are stored in `src/main/resources/pdfs` with a unique directory per template
* The root template must be named `main.html`

### Code Structure
The Kotlin type for PDF generation abstraction is called `Document`. It's a sealed class, so new document
types need to be created in `src/main/kotlin/io/zerobase/smarttracing/pdf/Documents.kt`. This pattern allows each
sub-type to provide separate arguments and processing, while also requiring that all sub-types are located in
the `Documents.kt`

Each sub-class of `Document` will need to override 2 properties:
* `val templateLocation: URL` - a Resource URL to the template-specific directory
* `val context: Context` - the data model for the template to render

#### Constructing instances of `Document` subclasses
Since rendering the template as a PDF requires a Thymeleaf engine and a Tidy, we want to avoid passing those
objects into REST resources just for passing it down (see the Law of Demeter). As such, there is a
`DocumentFactory` class that has those instances and should be passed around to the places that need to create
instances of `Document`. Each sub-type should have a constructor function on the factory.

To make sure that all instances are constructed through the factory, the constructor should be marked as `internal`
