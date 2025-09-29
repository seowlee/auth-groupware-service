-- 기본 팀
INSERT INTO groupware.teams (kr_name, en_name)
VALUES ('미지정', 'Unassigned')
ON CONFLICT (kr_name) DO NOTHING;

-- 최고관리자 사용자 (비밀번호 해시: pgcrypto 사용)
-- 비밀번호는 원하는 값으로 교체하세요.
INSERT INTO groupware.users (username, password, email, first_name, last_name, role, status, team_id)
VALUES ('admin',
        '$2a$10$XjL41BxElCHE8YRRymr1H.tfxWP4Eu2nLcaE2EamZifPzpmo27./S', -- bcrypt
        'admin@mail.com',
        'admin',
        'super',
        'SUPER_ADMIN',
        'ACTIVE',
        (SELECT id FROM groupware.teams WHERE kr_name = '미지정'))
ON CONFLICT (username, status) DO NOTHING;

