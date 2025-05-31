RABBITMQ_HOST = 'localhost'
EXCHANGE_NAME = 'system_bus'

ORDER_ROUTING_KEY_PREFIX = 'orders.'
CONFIRMATION_ROUTING_KEY_PREFIX = 'confirmations.to_team.'

ADMIN_BROADCAST_TEAMS_RK = 'admin.broadcast.teams'
ADMIN_BROADCAST_SUPPLIERS_RK = 'admin.broadcast.suppliers'
ADMIN_BROADCAST_ALL_RK = 'admin.broadcast.all'

ADMIN_MONITOR_QUEUE = 'q_admin_monitor'
ADMIN_MONITOR_BINDING_KEY = '#'

SUPPLIER_CAPABILITIES = {
    "Dostawca1": ["tlen", "buty"],
    "Dostawca2": ["tlen", "plecak"]
}

ALL_EQUIPMENT_TYPES = ["tlen", "buty", "plecak"]