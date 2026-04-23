---
name: courier-system-development
description: Provides domain knowledge, database schema guidelines, and actor use cases for developing the Courier System in Spring Boot and Svelte.
---

# Goal
To guide the development of the Courier System by providing strict entity structures, role definitions, and business logic requirements based on the MoSCoW analysis, Use Cases, and Database Schema.

# Instructions
* **Documentation Reference:**
    * When writing repository interfaces, SQL queries, or entity classes, you MUST strictly adhere to the schema defined in `.agents/docs/database-schema.dbml`.
    * For business logic flow, refer to the PlantUML use case diagrams located in the `.agents/docs/uml/` directory.
* **Entity Modeling:**
    * **Parcel (`parcels`):** Must track weight, dimensions, fragility, expected time, destination address, sender address, and status.
    * **Employee (`employes`):** Must track role, PESEL, login details, and address ID.
    * **Delivery Updates (`updates`):** Must track status changes, timestamps, and comments.
* **Actor Roles & Permissions:**
    * **Client:** Can check package status and send new packages.
    * **Worker:** Can verify packages, handle lost/destroyed/undelivered packages, generate reports, change ticket statuses, and add comments.
    * **Courier:** Can receive and deliver packages, mark packages as lost or destroyed, and add comments.
    * **Admin:** Can add, remove, and view information about workers and couriers.
* **Reporting System:** Implement a dedicated reporting system accessible by Workers.

# Constraints
* Do not allow Clients to directly communicate with Workers.
* All package rejections, failed deliveries, lost statuses, or destroyed statuses mandate adding a written comment.