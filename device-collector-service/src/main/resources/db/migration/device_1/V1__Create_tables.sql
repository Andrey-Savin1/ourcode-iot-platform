create table device_1.public.devices
(
    device_id   text primary key,
    device_type text,
    created_at  bigint,
    meta        varchar
);

COMMENT ON TABLE devices IS 'Хранение информации об устройствах';
COMMENT ON COLUMN devices.device_id IS 'Уникальный идентификатор устройства';
COMMENT ON COLUMN devices.device_type IS 'Тип устройства';
COMMENT ON COLUMN devices.created_at IS 'Время регистрации устройства в UNIX миллисекундах';
COMMENT ON COLUMN devices.meta IS 'Метаданные устройства в формате JSON';
