class Tenant:

    def __init__(self, tenant_id,  tenant_domain):
        self.tenant_id = tenant_id
        self.tenant_domain = tenant_domain
        self.service_name_subscription_map = {}

    def get_subscription(self, service_name):
        if service_name in self.service_name_subscription_map:
            return self.service_name_subscription_map[service_name]

        return None

    def is_subscribed(self, service_name):
        return service_name in self.service_name_subscription_map

    def add_subscription(self, subscription):
        self.service_name_subscription_map[subscription.service_name] = subscription

    def remove_subscription(self, service_name):
        if service_name in self.service_name_subscription_map:
            self.service_name_subscription_map.pop(service_name)


class Subscription:

    def __init__(self, service_name, cluster_ids):
        self.service_name = service_name
        self.cluster_ids = cluster_ids
        self.subscription_domain_map = {}

    def add_subscription_domain(self, domain_name, application_context):
        self.subscription_domain_map[domain_name] = SubscriptionDomain(domain_name, application_context)

    def remove_subscription_domain(self, domain_name):
        if domain_name in self.subscription_domain_map:
            self.subscription_domain_map.pop(domain_name)

    def subscription_domain_exists(self, domain_name):
        return domain_name in self.subscription_domain_map

    def get_subscription_domains(self):
        return self.subscription_domain_map.values()


class SubscriptionDomain:

    def __init__(self, domain_name, application_context):
        self.domain_name = domain_name
        self.application_context = application_context


class TenantContext:
    tenants = {}
    initialized = False
    tenant_domains = {"carbon.super": Tenant(-1234, "carbon.super")}

    @staticmethod
    def add_tenant(tenant):
        TenantContext.tenants[tenant.tenant_id] = tenant
        TenantContext.tenant_domains[tenant.tenant_domain] = tenant

    @staticmethod
    def remove_tenant(tenant_id):
        if tenant_id in TenantContext.tenants:
            tenant = TenantContext.get_tenant(tenant_id)
            TenantContext.tenants.pop(tenant.tenant_id)
            TenantContext.tenant_domains.pop(tenant.tenant_domain)

    @staticmethod
    def update(tenants):
        for tenant in tenants:
            TenantContext.add_tenant(tenant)

    @staticmethod
    def get_tenant(tenant_id):
        if tenant_id in TenantContext.tenants:
            return TenantContext.tenants[tenant_id]

        return None

    @staticmethod
    def get_tenant_by_domain(tenant_domain):
        if tenant_domain in TenantContext.tenant_domains:
            return TenantContext.tenant_domains[tenant_domain]

        return None