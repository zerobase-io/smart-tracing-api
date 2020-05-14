## Notifications
We are (or will be) sending a variety of notifications through (eventually) a variety of mediums. It's just email for now,
but SMS is on the road map, and we may have other options as well in the future like push notifications and phone calls.

This module has been designed to abstract the way the notifications are sent and the differing content as much as possible
from the calling site.

For notifications that require templates, we are standardizing on Thymeleaf as our template engine.

### Components

#### Notification
This is the sealed class that provides the API for triggering rendering. Each sub-class handles the actual rendering of its content.
We are using a sealed class instead of an interface to force all implementations to be co-located.

#### Notification Factory
To avoid passing around a template engine instance to all the places that want to create a notification, we
are hiding it behind a factory class. Each notification sub-class should have a constructor method on the factory and it's only
constructor should be marked as `internal` to prevent direct construction.

#### Notification Manager
This is the entry point for sending notifications. It accepts an instance of `Notification` and contact info. It will decide
what medium should be used for communication - for example, if no phone number is available for SMS, it will use email. The
notification will be asked to render itself to produce a string.

Once the notification has been rendered, individual providers of communication mediums will be invoked to transmit the notification.
