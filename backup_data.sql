--
-- PostgreSQL database dump
--

\restrict C8N3UVKk8Ar5TFf8NcyeeXGgEPqP9hksBTna7fSqahM6V6LfaQeiDxQoPc4iPxE

-- Dumped from database version 17.7 (Debian 17.7-3.pgdg13+1)
-- Dumped by pg_dump version 17.7 (Debian 17.7-3.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: partman; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA partman;


ALTER SCHEMA partman OWNER TO postgres;

--
-- Name: pg_partman; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_partman WITH SCHEMA partman;


--
-- Name: EXTENSION pg_partman; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pg_partman IS 'Extension to manage partitioned tables by time or ID';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: template_public_message; Type: TABLE; Schema: partman; Owner: postgres
--

CREATE TABLE partman.template_public_message (
    message_id integer NOT NULL,
    conversation_id integer NOT NULL,
    sender_id integer NOT NULL,
    content text,
    message_type character varying(20),
    metadata jsonb,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE partman.template_public_message OWNER TO postgres;

--
-- Name: account; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account (
    account_id integer NOT NULL,
    username character varying(50) NOT NULL,
    password_hash character varying(255) NOT NULL,
    email character varying(100),
    display_name character varying(100),
    avatar_url text,
    status character varying(20) DEFAULT 'offline'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    profile_settings jsonb
);


ALTER TABLE public.account OWNER TO postgres;

--
-- Name: account_account_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.account_account_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.account_account_id_seq OWNER TO postgres;

--
-- Name: account_account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.account_account_id_seq OWNED BY public.account.account_id;


--
-- Name: attachment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attachment (
    attachment_id bigint NOT NULL,
    message_id integer NOT NULL,
    file_url text NOT NULL,
    file_type character varying(50),
    file_size bigint,
    uploaded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.attachment OWNER TO postgres;

--
-- Name: attachment_attachment_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.attachment_attachment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.attachment_attachment_id_seq OWNER TO postgres;

--
-- Name: attachment_attachment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.attachment_attachment_id_seq OWNED BY public.attachment.attachment_id;


--
-- Name: conversation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.conversation (
    conversation_id bigint NOT NULL,
    name character varying(100),
    type character varying(20) DEFAULT 'private'::character varying NOT NULL,
    created_by integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    settings jsonb,
    metadata jsonb
);


ALTER TABLE public.conversation OWNER TO postgres;

--
-- Name: conversation_conversation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.conversation_conversation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.conversation_conversation_id_seq OWNER TO postgres;

--
-- Name: conversation_conversation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.conversation_conversation_id_seq OWNED BY public.conversation.conversation_id;


--
-- Name: forgot_password; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.forgot_password (
    id integer NOT NULL,
    account_id integer NOT NULL,
    token character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL
);


ALTER TABLE public.forgot_password OWNER TO postgres;

--
-- Name: forgot_password_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.forgot_password_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.forgot_password_id_seq OWNER TO postgres;

--
-- Name: forgot_password_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.forgot_password_id_seq OWNED BY public.forgot_password.id;


--
-- Name: message; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message (
    message_id integer NOT NULL,
    conversation_id integer NOT NULL,
    sender_id integer NOT NULL,
    content text,
    message_type character varying(20) DEFAULT 'text'::character varying,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
)
PARTITION BY RANGE (created_at);


ALTER TABLE public.message OWNER TO postgres;

--
-- Name: message_message_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.message_message_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.message_message_id_seq OWNER TO postgres;

--
-- Name: message_message_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.message_message_id_seq OWNED BY public.message.message_id;


--
-- Name: message_default; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_default (
    message_id integer DEFAULT nextval('public.message_message_id_seq'::regclass) NOT NULL,
    conversation_id integer NOT NULL,
    sender_id integer NOT NULL,
    content text,
    message_type character varying(20) DEFAULT 'text'::character varying,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.message_default OWNER TO postgres;

--
-- Name: message_p20260101; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_p20260101 (
    message_id integer DEFAULT nextval('public.message_message_id_seq'::regclass) NOT NULL,
    conversation_id integer NOT NULL,
    sender_id integer NOT NULL,
    content text,
    message_type character varying(20) DEFAULT 'text'::character varying,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.message_p20260101 OWNER TO postgres;

--
-- Name: message_partition_y2025m11; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_partition_y2025m11 (
    message_id integer DEFAULT nextval('public.message_message_id_seq'::regclass) NOT NULL,
    conversation_id integer NOT NULL,
    sender_id integer NOT NULL,
    content text,
    message_type character varying(20) DEFAULT 'text'::character varying,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.message_partition_y2025m11 OWNER TO postgres;

--
-- Name: message_partition_y2025m12; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_partition_y2025m12 (
    message_id integer DEFAULT nextval('public.message_message_id_seq'::regclass) NOT NULL,
    conversation_id integer NOT NULL,
    sender_id integer NOT NULL,
    content text,
    message_type character varying(20) DEFAULT 'text'::character varying,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.message_partition_y2025m12 OWNER TO postgres;

--
-- Name: message_reaction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_reaction (
    message_id bigint NOT NULL,
    account_id integer NOT NULL,
    reaction_type character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    id bigint NOT NULL,
    reaction_id integer NOT NULL,
    emoji character varying(255)
);


ALTER TABLE public.message_reaction OWNER TO postgres;

--
-- Name: message_reaction_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.message_reaction ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.message_reaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: message_reaction_reaction_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.message_reaction ALTER COLUMN reaction_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.message_reaction_reaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: message_status; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_status (
    message_id integer NOT NULL,
    account_id integer NOT NULL,
    status character varying(20) DEFAULT 'sent'::character varying,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    status_id bigint NOT NULL
);


ALTER TABLE public.message_status OWNER TO postgres;

--
-- Name: message_status_status_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.message_status ALTER COLUMN status_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.message_status_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: notification; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notification (
    notification_id bigint NOT NULL,
    account_id integer NOT NULL,
    type character varying(50) NOT NULL,
    content character varying(255),
    is_read boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.notification OWNER TO postgres;

--
-- Name: notification_notification_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.notification_notification_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notification_notification_id_seq OWNER TO postgres;

--
-- Name: notification_notification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.notification_notification_id_seq OWNED BY public.notification.notification_id;


--
-- Name: participants; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.participants (
    conversation_id bigint NOT NULL,
    account_id integer NOT NULL,
    role character varying(20) DEFAULT 'member'::character varying,
    joined_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    participant_id bigint NOT NULL,
    preferences jsonb
);


ALTER TABLE public.participants OWNER TO postgres;

--
-- Name: participants_participant_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.participants ALTER COLUMN participant_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.participants_participant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_contact; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_contact (
    contact_id bigint NOT NULL,
    owner_id integer NOT NULL,
    contact_user_id integer NOT NULL,
    status character varying(20) DEFAULT 'pending'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.user_contact OWNER TO postgres;

--
-- Name: user_contact_contact_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_contact_contact_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_contact_contact_id_seq OWNER TO postgres;

--
-- Name: user_contact_contact_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_contact_contact_id_seq OWNED BY public.user_contact.contact_id;


--
-- Name: user_device_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_device_token (
    id integer NOT NULL,
    token character varying(512) NOT NULL,
    account_id integer NOT NULL,
    last_updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.user_device_token OWNER TO postgres;

--
-- Name: user_device_token_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_device_token_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_device_token_id_seq OWNER TO postgres;

--
-- Name: user_device_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_device_token_id_seq OWNED BY public.user_device_token.id;


--
-- Name: verify_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.verify_token (
    id integer NOT NULL,
    email character varying(255) NOT NULL,
    token character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    password character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL
);


ALTER TABLE public.verify_token OWNER TO postgres;

--
-- Name: verify_token_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.verify_token ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.verify_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: message_default; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message ATTACH PARTITION public.message_default DEFAULT;


--
-- Name: message_p20260101; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message ATTACH PARTITION public.message_p20260101 FOR VALUES FROM ('2026-01-01 00:00:00') TO ('2026-02-01 00:00:00');


--
-- Name: message_partition_y2025m11; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message ATTACH PARTITION public.message_partition_y2025m11 FOR VALUES FROM ('2025-11-01 00:00:00') TO ('2025-12-01 00:00:00');


--
-- Name: message_partition_y2025m12; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message ATTACH PARTITION public.message_partition_y2025m12 FOR VALUES FROM ('2025-12-01 00:00:00') TO ('2026-01-01 00:00:00');


--
-- Name: account account_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account ALTER COLUMN account_id SET DEFAULT nextval('public.account_account_id_seq'::regclass);


--
-- Name: attachment attachment_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attachment ALTER COLUMN attachment_id SET DEFAULT nextval('public.attachment_attachment_id_seq'::regclass);


--
-- Name: conversation conversation_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation ALTER COLUMN conversation_id SET DEFAULT nextval('public.conversation_conversation_id_seq'::regclass);


--
-- Name: forgot_password id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.forgot_password ALTER COLUMN id SET DEFAULT nextval('public.forgot_password_id_seq'::regclass);


--
-- Name: message message_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message ALTER COLUMN message_id SET DEFAULT nextval('public.message_message_id_seq'::regclass);


--
-- Name: notification notification_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification ALTER COLUMN notification_id SET DEFAULT nextval('public.notification_notification_id_seq'::regclass);


--
-- Name: user_contact contact_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_contact ALTER COLUMN contact_id SET DEFAULT nextval('public.user_contact_contact_id_seq'::regclass);


--
-- Name: user_device_token id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_device_token ALTER COLUMN id SET DEFAULT nextval('public.user_device_token_id_seq'::regclass);


--
-- Data for Name: part_config; Type: TABLE DATA; Schema: partman; Owner: postgres
--

COPY partman.part_config (parent_table, control, time_encoder, time_decoder, partition_interval, partition_type, premake, automatic_maintenance, template_table, retention, retention_schema, retention_keep_index, retention_keep_table, epoch, constraint_cols, optimize_constraint, infinite_time_partitions, datetime_string, jobmon, sub_partition_set_full, undo_in_progress, inherit_privileges, constraint_valid, ignore_default_data, date_trunc_interval, maintenance_order, retention_keep_publication, maintenance_last_run, async_partitioning_in_progress) FROM stdin;
public.message	created_at	\N	\N	1 mon	range	1	on	partman.template_public_message	\N	\N	t	t	none	\N	30	f	YYYYMMDD	t	f	f	f	t	t	\N	\N	f	\N	\N
\.


--
-- Data for Name: part_config_sub; Type: TABLE DATA; Schema: partman; Owner: postgres
--

COPY partman.part_config_sub (sub_parent, sub_control, sub_time_encoder, sub_time_decoder, sub_partition_interval, sub_partition_type, sub_premake, sub_automatic_maintenance, sub_template_table, sub_retention, sub_retention_schema, sub_retention_keep_index, sub_retention_keep_table, sub_epoch, sub_constraint_cols, sub_optimize_constraint, sub_infinite_time_partitions, sub_jobmon, sub_inherit_privileges, sub_constraint_valid, sub_ignore_default_data, sub_default_table, sub_date_trunc_interval, sub_maintenance_order, sub_retention_keep_publication, sub_control_not_null) FROM stdin;
\.


--
-- Data for Name: template_public_message; Type: TABLE DATA; Schema: partman; Owner: postgres
--

COPY partman.template_public_message (message_id, conversation_id, sender_id, content, message_type, metadata, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: account; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.account (account_id, username, password_hash, email, display_name, avatar_url, status, created_at, profile_settings) FROM stdin;
1	alice	$2b$12$D.U.Rin08xW.9P201tV3OumxT.4aOqES6Y3.JdG.Sg0x9.x1a	alice@example.com	Alice Wonderland	https://i.pravatar.cc/150?u=alice	online	2025-10-22 08:35:26.137746	\N
2	bob	$2b$12$D.U.Rin08xW.9P201tV3OumxT.4aOqES6Y3.JdG.Sg0x9.x1b	bob@example.com	Bob The Builder	https://i.pravatar.cc/150?u=bob	offline	2025-10-22 08:35:26.137746	\N
4	david	$2b$12$D.U.Rin08xW.9P201tV3OumxT.4aOqES6Y3.JdG.Sg0x9.x1d	david@example.com	David Copperfield	https://i.pravatar.cc/150?u=david	online	2025-10-22 08:35:26.137746	\N
11	nguyenvan12	$2a$10$8Mk0k9Ta7eVk0j.h90gUdOYeHzUdj1t8wHiAv/v8L8f7HQg7ctIFu	vananhcute68@gmail.com	vananhcute68	\N	offline	2025-10-29 11:14:39.445761	\N
13	nguyentavan188		nguyentavan188@gmail.com	\N	\N	online	2025-12-01 14:31:07.75005	\N
\.


--
-- Data for Name: attachment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attachment (attachment_id, message_id, file_url, file_type, file_size, uploaded_at) FROM stdin;
1	1	/uploads/1762920298000_webchat (2).txt	text/plain	9000	2025-11-12 11:04:58.004139
2	7	/uploads/1762920809935_webchat (2).txt	text/plain	9000	2025-11-12 11:13:30.018603
3	22	/uploads/1764044460806_webchat (2).txt	text/plain	9000	2025-11-25 11:21:00.845423
\.


--
-- Data for Name: conversation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.conversation (conversation_id, name, type, created_by, created_at, settings, metadata) FROM stdin;
1	\N	PRIVATE	1	2025-10-22 08:35:26.137746	\N	\N
2	Project Phoenix Team	GROUP	1	2025-10-22 08:35:26.137746	\N	\N
3	con mực	GROUP	11	2025-11-06 09:07:10.031676	\N	\N
4	con mực	GROUP	11	2025-11-06 09:07:41.661152	\N	\N
5	legend	GROUP	11	2025-11-06 09:34:07.291982	\N	\N
6	legendOfdawn	GROUP	11	2025-11-06 09:45:21.491175	\N	\N
7	legendlol	GROUP	11	2025-11-06 10:45:24.857766	\N	\N
8	ohmyvenus	GROUP	11	2025-11-06 10:46:25.568756	\N	\N
9	concak	GROUP	11	2025-11-10 13:19:24.922954	\N	\N
10	MMB	GROUP	11	2025-11-12 16:52:21.223835	\N	\N
11	MBM	GROUP	11	2025-11-12 17:12:47.60387	\N	\N
12	em là ai	GROUP	11	2025-11-12 17:16:30.736449	\N	\N
13	who	GROUP	11	2025-11-12 17:20:24.867445	\N	\N
16	how	GROUP	11	2025-11-13 09:52:51.565513	\N	\N
17	how	GROUP	11	2025-11-13 09:54:35.33038	\N	\N
18	how	GROUP	11	2025-11-13 09:55:09.177271	\N	\N
20	concung	GROUP	11	2025-11-13 10:47:47.15442	\N	\N
21	emlacobe	GROUP	11	2025-11-19 09:49:35.176126	\N	\N
22	congico	GROUP	11	2025-11-19 13:45:23.383717	\N	\N
23	congicoádas	GROUP	11	2025-11-19 13:46:32.196268	\N	\N
25	khongcogi	GROUP	11	2025-11-19 13:52:42.252044	\N	\N
26	\N	PRIVATE	11	2025-11-19 14:27:31.418941	\N	\N
27	\N	PRIVATE	11	2025-11-19 16:40:57.434071	\N	\N
28	\N	PRIVATE	13	2025-12-01 14:41:26.105246	\N	\N
29	\N	PRIVATE	11	2025-12-03 11:24:53.638334	\N	\N
30	\N	PRIVATE	11	2025-12-03 11:26:32.71401	\N	\N
\.


--
-- Data for Name: forgot_password; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.forgot_password (id, account_id, token, expires_at) FROM stdin;
\.


--
-- Data for Name: message_default; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_default (message_id, conversation_id, sender_id, content, message_type, metadata, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: message_p20260101; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_p20260101 (message_id, conversation_id, sender_id, content, message_type, metadata, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: message_partition_y2025m11; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_partition_y2025m11 (message_id, conversation_id, sender_id, content, message_type, metadata, created_at, updated_at) FROM stdin;
1	1	11	 hả mẹo gì !	text	\N	2025-11-10 14:56:20.012105	2025-11-10 14:56:20.012105
2	1	11	 hả mẹo gì !	text	\N	2025-11-10 16:11:46.535164	2025-11-10 16:11:46.535164
3	1	11	 mẹo mày bé !	text	\N	2025-11-10 16:12:01.766116	2025-11-10 16:12:01.766116
4	1	11	 ton  !	text	\N	2025-11-11 11:14:04.772791	2025-11-11 11:14:04.772791
5	1	11	may nhin cai gì  !	text	\N	2025-11-11 11:45:47.727818	2025-11-11 11:45:47.727818
6	2	11	may nhin cái chóa gì  !	text	\N	2025-11-11 11:46:02.472602	2025-11-11 11:46:02.472602
7	2	11	webchat (2).txt	file	\N	2025-11-12 11:13:29.938245	2025-11-12 11:13:29.938896
8	2	11	what are you looking for !	text	\N	2025-11-12 11:39:02.143337	2025-11-12 11:39:02.143337
9	2	11	 hả mẹo gì!	text	\N	2025-11-12 11:56:40.201477	2025-11-12 11:56:40.201477
10	2	11	 sl!	text	\N	2025-11-12 13:27:14.702485	2025-11-12 13:27:14.702485
11	2	11	 slxvxz!	text	\N	2025-11-12 13:34:15.896344	2025-11-12 13:34:15.896344
12	2	11	 em là không thể !	text	\N	2025-11-12 13:35:21.900517	2025-11-12 13:35:21.900517
13	2	11	 em là không thể alo !	text	\N	2025-11-12 13:52:23.676152	2025-11-12 13:52:23.676152
14	2	11	 haha !	text	{"replyInfo": {"repliedToContent": " em là không thể !", "repliedToSenderId": 11, "repliedToMessageId": 12, "repliedToSenderName": "vananhcute68"}}	2025-11-12 14:31:59.047077	2025-11-12 14:31:59.047077
15	2	11	 haha so funny !	text	\N	2025-11-12 14:43:56.718559	2025-11-12 14:43:56.718559
16	2	11	 haha so funny !	text	\N	2025-11-12 15:26:14.122345	2025-11-12 15:26:14.122345
17	2	11	 hả mẹo gì !	text	\N	2025-11-12 15:42:53.121321	2025-11-12 15:42:53.121321
18	2	11	 hả mẹo gì !	text	\N	2025-11-24 15:43:02.064031	2025-11-24 15:43:02.064031
19	2	11	 không thể gì !	text	{"replyInfo": {"repliedToContent": " em là không thể !", "repliedToSenderId": 11, "repliedToMessageId": 12, "repliedToSenderName": "vananhcute68"}}	2025-11-25 11:15:23.947856	2025-11-25 11:15:23.948855
20	2	11	 không thể cmm !	text	{"replyInfo": {"repliedToContent": " em là không thể !", "repliedToSenderId": 11, "repliedToMessageId": 12, "repliedToSenderName": "vananhcute68"}}	2025-11-25 11:17:32.63807	2025-11-25 11:17:32.63807
22	3	11	webchat (2).txt	file	\N	2025-11-25 11:21:00.825561	2025-11-25 11:21:00.825561
23	3	11	 khong on !	text	\N	2025-11-26 09:29:17.769431	2025-11-26 09:29:17.769431
\.


--
-- Data for Name: message_partition_y2025m12; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_partition_y2025m12 (message_id, conversation_id, sender_id, content, message_type, metadata, created_at, updated_at) FROM stdin;
25	28	13	 cai gi co may noi lai tao xem nao !	text	\N	2025-12-01 14:41:41.807294	2025-12-01 14:41:41.807294
26	28	13	 cai gi co!	text	\N	2025-12-01 16:11:51.346152	2025-12-01 16:11:51.346152
27	21	11	 cai gi co!	text	\N	2025-12-03 11:28:34.787546	2025-12-03 11:28:34.787546
28	1	11	 cai gi co!	text	\N	2025-12-04 11:53:42.903942	2025-12-04 11:53:42.903942
\.


--
-- Data for Name: message_reaction; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_reaction (message_id, account_id, reaction_type, created_at, id, reaction_id, emoji) FROM stdin;
15	11	❤️	2025-11-04 11:08:43.610599	1	1	\N
4	11	4	2025-11-12 10:29:38.319248	2	2	\N
\.


--
-- Data for Name: message_status; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_status (message_id, account_id, status, updated_at, status_id) FROM stdin;
1	11	sent	2025-11-10 14:56:20.096074	1
2	11	sent	2025-11-10 16:11:46.608178	2
3	11	sent	2025-11-10 16:12:01.770114	3
4	11	sent	2025-11-11 11:14:04.856475	4
5	11	sent	2025-11-11 11:45:47.864784	5
6	11	sent	2025-11-11 11:46:02.476601	6
8	11	sent	2025-11-12 11:39:02.223331	7
9	11	sent	2025-11-12 11:56:40.276179	8
10	11	sent	2025-11-12 13:27:14.795088	9
11	11	sent	2025-11-12 13:34:15.948371	10
12	11	sent	2025-11-12 13:35:21.904516	11
13	11	sent	2025-11-12 13:52:23.730741	12
14	11	sent	2025-11-12 14:31:59.105077	13
15	11	sent	2025-11-12 14:43:56.722563	14
16	11	sent	2025-11-12 15:26:14.131339	15
17	11	sent	2025-11-12 15:42:53.19032	16
18	11	sent	2025-11-24 15:43:02.215033	17
19	11	sent	2025-11-25 11:15:24.423528	18
20	11	sent	2025-11-25 11:17:32.659783	19
21	11	sent	2025-11-25 11:18:43.280688	20
23	11	sent	2025-11-26 09:29:17.905879	21
24	11	sent	2025-11-26 13:58:39.717322	22
25	13	sent	2025-12-01 14:41:41.851248	23
26	13	sent	2025-12-01 16:11:51.368657	24
27	11	sent	2025-12-03 11:28:34.817815	25
28	11	sent	2025-12-04 11:53:43.04104	26
\.


--
-- Data for Name: notification; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notification (notification_id, account_id, type, content, is_read, created_at) FROM stdin;
1	4	friend_request	Bob The Builder đã gửi cho bạn một lời mời kết bạn.	f	2025-10-22 08:35:26.137746
\.


--
-- Data for Name: participants; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.participants (conversation_id, account_id, role, joined_at, participant_id, preferences) FROM stdin;
1	1	member	2025-10-22 08:35:26.137746	1	\N
1	2	member	2025-10-22 08:35:26.137746	2	\N
2	1	owner	2025-10-22 08:35:26.137746	3	\N
2	2	admin	2025-10-22 08:35:26.137746	4	\N
2	11	member	2025-11-05 11:46:04.532513	7	\N
3	11	admin	2025-11-06 09:07:10.19671	8	\N
4	11	admin	2025-11-06 09:07:41.665149	9	\N
4	2	member	2025-11-06 09:07:41.689178	10	\N
3	1	member	2025-11-06 09:21:27.015194	11	\N
3	2	member	2025-11-06 09:21:27.095737	12	\N
3	4	member	2025-11-06 09:21:43.853252	13	\N
4	1	member	2025-11-06 09:28:31.679399	14	\N
4	4	member	2025-11-06 09:28:31.728401	15	\N
5	11	admin	2025-11-06 09:34:07.394001	16	\N
5	2	member	2025-11-06 09:34:07.415982	17	\N
5	1	member	2025-11-06 09:35:33.970077	20	\N
5	4	member	2025-11-06 09:39:40.98107	23	\N
6	11	admin	2025-11-06 09:45:21.555592	24	\N
6	2	member	2025-11-06 09:45:21.559592	25	\N
6	4	member	2025-11-06 09:45:31.653445	26	\N
7	11	admin	2025-11-06 10:45:24.955779	27	\N
8	11	admin	2025-11-06 10:46:25.658755	29	\N
8	2	member	2025-11-06 10:46:25.67595	30	\N
7	4	member	2025-11-06 10:55:00.715075	31	\N
9	11	admin	2025-11-10 13:19:25.045395	32	\N
9	2	member	2025-11-10 13:19:25.070423	33	\N
9	4	member	2025-11-12 10:47:49.392549	34	\N
10	11	admin	2025-11-12 16:52:21.320847	35	\N
10	2	member	2025-11-12 16:52:21.331831	36	\N
11	11	admin	2025-11-12 17:12:47.700286	37	\N
11	2	member	2025-11-12 17:12:47.717277	38	\N
12	11	admin	2025-11-12 17:16:30.836474	39	\N
12	2	member	2025-11-12 17:16:30.852474	40	\N
13	11	admin	2025-11-12 17:20:24.941443	41	\N
13	2	member	2025-11-12 17:20:24.957444	42	\N
16	11	admin	2025-11-13 09:52:51.649509	45	\N
16	2	member	2025-11-13 09:52:51.66751	46	\N
17	11	admin	2025-11-13 09:54:35.332381	47	\N
17	2	member	2025-11-13 09:54:35.335381	48	\N
20	11	admin	2025-11-13 10:47:47.22342	53	\N
20	2	member	2025-11-13 10:47:47.239422	54	\N
18	2	admin	2025-11-13 09:55:09.184277	50	\N
21	11	admin	2025-11-19 09:49:35.680493	55	\N
21	2	member	2025-11-19 09:49:35.744102	56	\N
9	1	member	2025-11-19 11:07:34.542618	57	\N
22	11	admin	2025-11-19 13:45:23.508717	58	\N
22	2	member	2025-11-19 13:45:23.54072	59	\N
23	11	admin	2025-11-19 13:46:32.198854	60	\N
25	11	admin	2025-11-19 13:52:42.254045	62	\N
25	1	member	2025-11-19 13:52:42.276042	63	\N
25	2	member	2025-11-19 13:52:42.27906	64	\N
26	11	member	2025-11-19 14:27:31.497019	65	\N
26	1	member	2025-11-19 14:27:31.502017	66	\N
27	11	member	2025-11-19 16:40:57.470076	67	\N
27	2	member	2025-11-19 16:40:57.473076	68	\N
\.


--
-- Data for Name: user_contact; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_contact (contact_id, owner_id, contact_user_id, status, created_at) FROM stdin;
1	1	2	accepted	2025-10-22 08:35:26.137746
2	2	1	accepted	2025-10-22 08:35:26.137746
5	2	4	pending	2025-10-22 08:35:26.137746
6	11	13	accepted	2025-12-03 11:24:53.527273
7	13	11	accepted	2025-12-03 11:24:53.605238
8	11	1	accepted	2025-12-03 11:26:13.395078
9	1	11	accepted	2025-12-03 11:26:13.402899
10	11	2	accepted	2025-12-03 11:26:25.397354
11	2	11	accepted	2025-12-03 11:26:25.404354
12	11	4	accepted	2025-12-03 11:26:32.695499
13	4	11	accepted	2025-12-03 11:26:32.703504
\.


--
-- Data for Name: user_device_token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_device_token (id, token, account_id, last_updated) FROM stdin;
1	laptop-fcm-token-12345ABCXYZ	11	2025-12-01 16:08:25.542087
2	laptop-fcm-token-eyioashd321kj312k123	11	2025-12-02 08:46:47.706162
3	laptop-fcm-token-eyakdba4j23m4b2j34bd	13	2025-12-02 08:50:35.47798
\.


--
-- Data for Name: verify_token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.verify_token (id, email, token, expires_at, password, full_name) FROM stdin;
17	newuser@example.com	unique_verification_token_string_12345	2025-10-22 09:35:26.137746	$2b$12$....(new_password_hash)	New User
\.


--
-- Name: account_account_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.account_account_id_seq', 13, true);


--
-- Name: attachment_attachment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.attachment_attachment_id_seq', 3, true);


--
-- Name: conversation_conversation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.conversation_conversation_id_seq', 30, true);


--
-- Name: forgot_password_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.forgot_password_id_seq', 2, true);


--
-- Name: message_message_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.message_message_id_seq', 28, true);


--
-- Name: message_reaction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.message_reaction_id_seq', 2, true);


--
-- Name: message_reaction_reaction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.message_reaction_reaction_id_seq', 2, true);


--
-- Name: message_status_status_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.message_status_status_id_seq', 26, true);


--
-- Name: notification_notification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.notification_notification_id_seq', 1, true);


--
-- Name: participants_participant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.participants_participant_id_seq', 68, true);


--
-- Name: user_contact_contact_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_contact_contact_id_seq', 13, true);


--
-- Name: user_device_token_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_device_token_id_seq', 3, true);


--
-- Name: verify_token_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.verify_token_id_seq', 42, true);


--
-- Name: account account_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_email_key UNIQUE (email);


--
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (account_id);


--
-- Name: account account_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_username_key UNIQUE (username);


--
-- Name: attachment attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (attachment_id);


--
-- Name: conversation conversation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation
    ADD CONSTRAINT conversation_pkey PRIMARY KEY (conversation_id);


--
-- Name: forgot_password forgot_password_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.forgot_password
    ADD CONSTRAINT forgot_password_pkey PRIMARY KEY (id);


--
-- Name: forgot_password forgot_password_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.forgot_password
    ADD CONSTRAINT forgot_password_token_key UNIQUE (token);


--
-- Name: message message_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (message_id, created_at);


--
-- Name: message_default message_default_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_default
    ADD CONSTRAINT message_default_pkey PRIMARY KEY (message_id, created_at);


--
-- Name: message_p20260101 message_p20260101_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_p20260101
    ADD CONSTRAINT message_p20260101_pkey PRIMARY KEY (message_id, created_at);


--
-- Name: message_partition_y2025m11 message_partition_y2025m11_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_partition_y2025m11
    ADD CONSTRAINT message_partition_y2025m11_pkey PRIMARY KEY (message_id, created_at);


--
-- Name: message_partition_y2025m12 message_partition_y2025m12_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_partition_y2025m12
    ADD CONSTRAINT message_partition_y2025m12_pkey PRIMARY KEY (message_id, created_at);


--
-- Name: message_reaction message_reaction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reaction
    ADD CONSTRAINT message_reaction_pkey PRIMARY KEY (message_id, account_id);


--
-- Name: message_status message_status_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_status
    ADD CONSTRAINT message_status_pkey PRIMARY KEY (message_id, account_id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (notification_id);


--
-- Name: participants participants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participants
    ADD CONSTRAINT participants_pkey PRIMARY KEY (conversation_id, account_id);


--
-- Name: participants uk_conversation_account; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participants
    ADD CONSTRAINT uk_conversation_account UNIQUE (conversation_id, account_id);


--
-- Name: user_contact unique_contact; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_contact
    ADD CONSTRAINT unique_contact UNIQUE (owner_id, contact_user_id);


--
-- Name: user_contact unique_contact_constraint; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_contact
    ADD CONSTRAINT unique_contact_constraint UNIQUE (owner_id, contact_user_id);


--
-- Name: user_contact user_contact_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_contact
    ADD CONSTRAINT user_contact_pkey PRIMARY KEY (contact_id);


--
-- Name: user_device_token user_device_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_device_token
    ADD CONSTRAINT user_device_token_pkey PRIMARY KEY (id);


--
-- Name: user_device_token user_device_token_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_device_token
    ADD CONSTRAINT user_device_token_token_key UNIQUE (token);


--
-- Name: verify_token verify_token_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verify_token
    ADD CONSTRAINT verify_token_email_key UNIQUE (email);


--
-- Name: verify_token verify_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verify_token
    ADD CONSTRAINT verify_token_pkey PRIMARY KEY (id);


--
-- Name: verify_token verify_token_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verify_token
    ADD CONSTRAINT verify_token_token_key UNIQUE (token);


--
-- Name: idx_attachment_message_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attachment_message_id ON public.attachment USING btree (message_id);


--
-- Name: idx_conversation_settings_gin; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_conversation_settings_gin ON public.conversation USING gin (settings);


--
-- Name: idx_message_metadata_gin; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_message_metadata_gin ON ONLY public.message USING gin (metadata);


--
-- Name: idx_user_device_token_account_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_device_token_account_id ON public.user_device_token USING btree (account_id);


--
-- Name: message_default_metadata_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX message_default_metadata_idx ON public.message_default USING gin (metadata);


--
-- Name: message_p20260101_metadata_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX message_p20260101_metadata_idx ON public.message_p20260101 USING gin (metadata);


--
-- Name: message_partition_y2025m11_metadata_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX message_partition_y2025m11_metadata_idx ON public.message_partition_y2025m11 USING gin (metadata);


--
-- Name: message_partition_y2025m12_metadata_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX message_partition_y2025m12_metadata_idx ON public.message_partition_y2025m12 USING gin (metadata);


--
-- Name: message_default_metadata_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_message_metadata_gin ATTACH PARTITION public.message_default_metadata_idx;


--
-- Name: message_default_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.message_pkey ATTACH PARTITION public.message_default_pkey;


--
-- Name: message_p20260101_metadata_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_message_metadata_gin ATTACH PARTITION public.message_p20260101_metadata_idx;


--
-- Name: message_p20260101_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.message_pkey ATTACH PARTITION public.message_p20260101_pkey;


--
-- Name: message_partition_y2025m11_metadata_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_message_metadata_gin ATTACH PARTITION public.message_partition_y2025m11_metadata_idx;


--
-- Name: message_partition_y2025m11_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.message_pkey ATTACH PARTITION public.message_partition_y2025m11_pkey;


--
-- Name: message_partition_y2025m12_metadata_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_message_metadata_gin ATTACH PARTITION public.message_partition_y2025m12_metadata_idx;


--
-- Name: message_partition_y2025m12_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.message_pkey ATTACH PARTITION public.message_partition_y2025m12_pkey;


--
-- Name: conversation conversation_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation
    ADD CONSTRAINT conversation_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: message fk5k0olkd82xhjehyhuy44pqi4c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE public.message
    ADD CONSTRAINT fk5k0olkd82xhjehyhuy44pqi4c FOREIGN KEY (sender_id) REFERENCES public.account(account_id);


--
-- Name: message fk6yskk3hxw5sklwgi25y6d5u1l; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE public.message
    ADD CONSTRAINT fk6yskk3hxw5sklwgi25y6d5u1l FOREIGN KEY (conversation_id) REFERENCES public.conversation(conversation_id);


--
-- Name: message fk_message_account; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE public.message
    ADD CONSTRAINT fk_message_account FOREIGN KEY (sender_id) REFERENCES public.account(account_id);


--
-- Name: user_device_token fk_user_device_token_account; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_device_token
    ADD CONSTRAINT fk_user_device_token_account FOREIGN KEY (account_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: forgot_password forgot_password_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.forgot_password
    ADD CONSTRAINT forgot_password_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: message message_conversation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE public.message
    ADD CONSTRAINT message_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.conversation(conversation_id) ON DELETE CASCADE;


--
-- Name: message_reaction message_reaction_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reaction
    ADD CONSTRAINT message_reaction_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: message message_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE public.message
    ADD CONSTRAINT message_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: message_status message_status_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_status
    ADD CONSTRAINT message_status_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: notification notification_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: participants participants_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participants
    ADD CONSTRAINT participants_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: participants participants_conversation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participants
    ADD CONSTRAINT participants_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.conversation(conversation_id) ON DELETE CASCADE;


--
-- Name: user_contact user_contact_contact_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_contact
    ADD CONSTRAINT user_contact_contact_user_id_fkey FOREIGN KEY (contact_user_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- Name: user_contact user_contact_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_contact
    ADD CONSTRAINT user_contact_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.account(account_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict C8N3UVKk8Ar5TFf8NcyeeXGgEPqP9hksBTna7fSqahM6V6LfaQeiDxQoPc4iPxE

